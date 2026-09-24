package com.beamformers.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEColor;

import com.beamformers.BeamFormers;
import com.beamformers.BeamFormersConfig;
import com.beamformers.former.Beam;
import com.beamformers.former.BeamEnd;
import com.beamformers.former.BeamRegistry;
import com.beamformers.former.IBeamSource;
import com.beamformers.former.PathWatcher;

/**
 * A tower linked by memory card to other towers, at any angle, each link a dense connection while nothing stands
 * between the two crystals. Both towers keep the link; the one that made the connection owns it and draws it.
 */
public class TileBeamTower extends TileMounted implements IBeamSource, PathWatcher.Watcher {

    /** The crystal's centre stands this far out along the facing, and this far along the model's up. */
    private static final double CRYSTAL = 4.5 / 16;
    private static final double CRYSTAL_UP = -0.25 / 16;
    private static final float RADIUS = 1 / 32f;
    private static final float GLOW = 5 / 64f;
    private static final double POWER_PER_LINK = 0.5;

    private final Map<BlockPos, Link> links = new LinkedHashMap<>();
    private boolean hidden;
    private int active;

    private final List<Beam> clientBeams = new ArrayList<>();

    public TileBeamTower() {
        super(14, 8);
        this.getProxy().setIdlePowerUsage(0);
    }

    private Vec3d crystal() {
        return new Vec3d(this.pos).add(0.5, 0.5, 0.5).add(new Vec3d(this.getForward().getDirectionVec()).scale(CRYSTAL))
                .add(new Vec3d(this.getUp().getDirectionVec()).scale(CRYSTAL_UP));
    }

    @Override
    protected void update() {
        final IGridNode node = this.getReadyNode();
        if (node == null) {
            return;
        }
        final Set<BlockPos> watched = new HashSet<>();
        final Set<ChunkPos> unloaded = new HashSet<>();
        boolean changed = false;
        int active = 0;

        for (Iterator<Map.Entry<BlockPos, Link>> it = this.links.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<BlockPos, Link> entry = it.next();
            final BlockPos target = entry.getKey();
            final Link link = entry.getValue();
            if (!this.world.isBlockLoaded(target)) {
                unloaded.add(new ChunkPos(target));
                changed |= this.drop(link, null);
                continue;
            }
            final TileEntity te = this.world.getTileEntity(target);
            final TileBeamTower other = te instanceof TileBeamTower ? (TileBeamTower) te : null;
            final Link back = other == null ? null : other.links.get(this.pos);
            if (back == null) {
                this.drop(link, null);
                it.remove();
                changed = true;
                continue;
            }

            final IGridNode otherNode = other.getReadyNode();
            link.blocker = blocker(this.world, this.crystal(), other.crystal(), this.pos, target, watched, unloaded);
            if (otherNode == null || link.blocker != null || !this.getMountedColor().matches(other.getMountedColor())) {
                changed |= this.drop(link, other);
                continue;
            }

            if (!live(link, node)) {
                if (live(back, otherNode)) {
                    link.connection = back.connection;
                    link.owner = false;
                } else {
                    try {
                        link.connection = AEApi.instance().grid().createGridConnection(node, otherNode);
                    } catch (FailedConnectionException e) {
                        BeamFormers.log.debug("A beam from the tower at {} could not join {}: {}", this.pos, target,
                                e.getMessage());
                        continue;
                    }
                    back.connection = link.connection;
                    link.owner = true;
                    back.owner = false;
                }
                changed = true;
            }
            active++;

            final AEColor color = beamColor(this, other);
            final TileBeamTower owner = link.owner ? this : other;
            final Link owned = link.owner ? link : back;
            if (owned.color != color) {
                owned.color = color;
                owner.beamChanged();
            }
        }

        PathWatcher.INSTANCE.watch(this, this.world, watched, this.getColorSource(), unloaded);
        if (active != this.active) {
            this.active = active;
            this.getProxy().setIdlePowerUsage(active * POWER_PER_LINK);
            changed = true;
        }
        if (changed) {
            this.beamChanged();
        }
    }

    private static boolean live(Link link, IGridNode node) {
        return link.connection != null && node.getConnections().contains(link.connection);
    }

    // Drops the connection on both towers and wakes the other one to look again.
    private boolean drop(Link link, @Nullable TileBeamTower other) {
        if (link.connection == null) {
            return false;
        }
        final IGridNode node = this.getProxy().getNode();
        if (node != null && node.getConnections().contains(link.connection)) {
            link.connection.destroy();
        }
        link.connection = null;
        link.owner = false;
        if (other != null) {
            final Link back = other.links.get(this.pos);
            if (back != null) {
                back.connection = null;
                back.owner = false;
            }
            other.beamChanged();
            other.wake();
        }
        return true;
    }

    // A coloured end beats a transparent one.
    private static AEColor beamColor(TileBeamTower a, TileBeamTower b) {
        final AEColor color = a.getMountedColor();
        return color != AEColor.TRANSPARENT ? color : b.getMountedColor();
    }

    // Walks every block the line crosses but the two towers', collecting them to be watched.
    @Nullable
    private static BlockPos blocker(World world, Vec3d from, Vec3d to, BlockPos a, BlockPos b, Set<BlockPos> watched,
            Set<ChunkPos> unloaded) {
        final Vec3d d = to.subtract(from);
        int x = MathHelper.floor(from.x);
        int y = MathHelper.floor(from.y);
        int z = MathHelper.floor(from.z);
        final int sx = (int) Math.signum(d.x);
        final int sy = (int) Math.signum(d.y);
        final int sz = (int) Math.signum(d.z);
        final double dx = sx == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.x);
        final double dy = sy == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.y);
        final double dz = sz == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.z);
        double tx = sx == 0 ? Double.MAX_VALUE : (sx > 0 ? x + 1 - from.x : from.x - x) * dx;
        double ty = sy == 0 ? Double.MAX_VALUE : (sy > 0 ? y + 1 - from.y : from.y - y) * dy;
        double tz = sz == 0 ? Double.MAX_VALUE : (sz > 0 ? z + 1 - from.z : from.z - z) * dz;

        for (int steps = 0; steps < 4 * (BeamFormersConfig.towerRange + 2); steps++) {
            final BlockPos p = new BlockPos(x, y, z);
            if (p.equals(b)) {
                return null;
            }
            if (!p.equals(a)) {
                if (!world.isBlockLoaded(p)) {
                    unloaded.add(new ChunkPos(p));
                    return p;
                }
                watched.add(p);
                if (BeamEnd.blocks(world, p)) {
                    return p;
                }
            }
            if (tx <= ty && tx <= tz) {
                x += sx;
                tx += dx;
            } else if (ty <= tz) {
                y += sy;
                ty += dy;
            } else {
                z += sz;
                tz += dz;
            }
        }
        return b;
    }

    /** What a memory card keeps of this tower. */
    public NBTTagCompound cardData() {
        final NBTTagCompound data = new NBTTagCompound();
        data.setLong("tower", this.pos.toLong());
        data.setInteger("dim", this.world.provider.getDimension());
        return data;
    }

    /** Links this tower to the one a memory card holds, or unlinks the two; says how it went. */
    public ITextComponent toggleLink(NBTTagCompound card) {
        final BlockPos target = BlockPos.fromLong(card.getLong("tower"));
        if (card.getInteger("dim") != this.world.provider.getDimension()) {
            return new TextComponentTranslation("chat.beamformers.tower_other_dimension");
        }
        if (target.equals(this.pos)) {
            return new TextComponentTranslation("chat.beamformers.tower_same");
        }
        final TileEntity te = this.world.isBlockLoaded(target) ? this.world.getTileEntity(target) : null;
        if (!(te instanceof TileBeamTower)) {
            return new TextComponentTranslation("chat.beamformers.tower_missing");
        }
        final TileBeamTower other = (TileBeamTower) te;

        final Link link = this.links.get(target);
        if (link != null) {
            this.drop(link, other);
            this.links.remove(target);
            other.links.remove(this.pos);
            this.linksChanged(other);
            return new TextComponentTranslation("chat.beamformers.tower_unlinked");
        }
        final int range = BeamFormersConfig.towerRange;
        if (this.pos.distanceSq(target) > range * range) {
            return new TextComponentTranslation("chat.beamformers.tower_too_far", range);
        }
        final int max = BeamFormersConfig.towerMaxLinks;
        if (this.links.size() >= max || other.links.size() >= max) {
            return new TextComponentTranslation("chat.beamformers.tower_full", max);
        }
        this.links.put(target, new Link());
        other.links.put(this.pos, new Link());
        this.linksChanged(other);
        return new TextComponentTranslation("chat.beamformers.tower_linked");
    }

    private void linksChanged(TileBeamTower other) {
        this.beamChanged();
        other.beamChanged();
        this.wake();
        other.wake();
    }

    @Override
    public boolean toggleHidden() {
        this.hidden = !this.hidden;
        this.beamChanged();
        this.forEachLoadedPartner(TileBeamTower::beamChanged);
        return this.hidden;
    }

    private void forEachLoadedPartner(Consumer<TileBeamTower> action) {
        for (BlockPos target : this.links.keySet()) {
            if (this.world.isBlockLoaded(target)) {
                final TileEntity te = this.world.getTileEntity(target);
                if (te instanceof TileBeamTower) {
                    action.accept((TileBeamTower) te);
                }
            }
        }
    }

    @Override
    public void onChunkUnload() {
        this.leave();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        this.leave();
        super.invalidate();
    }

    // The partners wake to see this tower gone; the links are kept for when it loads again.
    private void leave() {
        BeamRegistry.remove(this);
        if (this.world != null && !this.world.isRemote) {
            PathWatcher.INSTANCE.unwatch(this);
            this.forEachLoadedPartner(TileMounted::wake);
        }
    }

    @Override
    protected void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        final List<Vec3d> ends = new ArrayList<>();
        final List<AEColor> colors = new ArrayList<>();
        if (!this.hidden && this.getProxy().isPowered()) {
            for (Map.Entry<BlockPos, Link> entry : this.links.entrySet()) {
                final Link link = entry.getValue();
                final TileEntity te = link.owner && link.connection != null ? this.world.getTileEntity(entry.getKey()) : null;
                if (te instanceof TileBeamTower && !((TileBeamTower) te).hidden) {
                    ends.add(((TileBeamTower) te).crystal());
                    colors.add(link.color);
                }
            }
        }
        data.writeByte(ends.size());
        for (int i = 0; i < ends.size(); i++) {
            final Vec3d end = ends.get(i);
            data.writeDouble(end.x);
            data.writeDouble(end.y);
            data.writeDouble(end.z);
            data.writeByte(colors.get(i).ordinal());
        }
    }

    @Override
    protected boolean readFromStream(ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        this.clientBeams.clear();
        final int count = data.readUnsignedByte();
        final Vec3d start = this.crystal();
        for (int i = 0; i < count; i++) {
            final Vec3d end = new Vec3d(data.readDouble(), data.readDouble(), data.readDouble());
            this.clientBeams.add(new Beam(start, end, RADIUS, GLOW, AEColor.values()[data.readByte()], 0));
        }
        BeamRegistry.update(this, !this.clientBeams.isEmpty());
        return changed;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        final NBTTagList list = new NBTTagList();
        this.links.keySet().forEach(target -> list.appendTag(new NBTTagLong(target.toLong())));
        data.setTag("links", list);
        if (this.hidden) {
            data.setBoolean("hideBeam", true);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.links.clear();
        final NBTTagList list = data.getTagList("links", Constants.NBT.TAG_LONG);
        for (int i = 0; i < list.tagCount(); i++) {
            this.links.put(BlockPos.fromLong(((NBTTagLong) list.get(i)).getLong()), new Link());
        }
        this.hidden = data.getBoolean("hideBeam");
    }

    @Override
    public World getSourceWorld() {
        return this.world;
    }

    @Override
    public boolean isSourceInWorld() {
        return this.isInWorld();
    }

    @Override
    public void addBeams(List<Beam> out) {
        out.addAll(this.clientBeams);
    }

    public boolean isHidden() {
        return this.hidden;
    }

    /** Server side: every link as the last look found it. */
    public List<LinkState> getLinkStates() {
        final List<LinkState> states = new ArrayList<>();
        final IGridNode node = this.getProxy().getNode();
        this.links.forEach((target, link) -> {
            final boolean active = node != null && live(link, node);
            states.add(new LinkState(target, active, active ? link.connection.getUsedChannels() : 0, link.blocker));
        });
        return states;
    }

    public static final class LinkState {

        public final BlockPos target;
        public final boolean active;
        public final int usedChannels;
        @Nullable
        public final BlockPos blocker;

        LinkState(BlockPos target, boolean active, int usedChannels, @Nullable BlockPos blocker) {
            this.target = target;
            this.active = active;
            this.usedChannels = usedChannels;
            this.blocker = blocker;
        }
    }

    private static final class Link {

        @Nullable
        IGridConnection connection;
        boolean owner;
        AEColor color = AEColor.TRANSPARENT;
        @Nullable
        BlockPos blocker;
    }

}
