package com.beamformers.former;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;

import com.beamformers.BeamFormers;
import com.beamformers.BeamFormersConfig;

/**
 * One end of a beam, and the pairing both kinds of former share. Of a joined pair, the end that made the
 * connection owns it: it measures the beam and the client draws it from there.
 */
public final class BeamEnd implements IBeamSource, PathWatcher.Watcher {

    /** How far each end of a beam reaches into its former, so no gap shows at the tip. */
    private static final double SUNK = 1 / 32d;

    private final IBeamFormer former;

    @Nullable
    private BeamEnd partner;
    @Nullable
    private IGridConnection connection;
    private boolean owner;
    private int blocksBetween;
    private AEColor color = AEColor.TRANSPARENT;
    private boolean hidden;

    private float clientLength;
    private AEColor clientColor = AEColor.TRANSPARENT;

    public BeamEnd(IBeamFormer former) {
        this.former = former;
    }

    public IBeamFormer getFormer() {
        return this.former;
    }

    @Nullable
    public BeamEnd getPartner() {
        return this.partner;
    }

    @Nullable
    public IGridConnection getConnection() {
        return this.connection;
    }

    public boolean isOwner() {
        return this.owner;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    /** Looks along the facing, then joins, keeps or drops the former found there. Server side, on a grid tick. */
    public void update() {
        final IGridNode node = this.former.getFormerNode();
        if (node == null) {
            return;
        }

        final World world = this.former.getFormerWorld();
        final EnumFacing facing = this.former.getFacing();
        final List<BlockPos> watched = new ArrayList<>();
        ChunkPos unloaded = null;
        BeamEnd found = null;

        BlockPos pos = this.former.getFormerPos();
        for (int i = 0; i < BeamFormersConfig.formerRange; i++) {
            pos = pos.offset(facing);
            if (!world.isBlockLoaded(pos)) {
                unloaded = new ChunkPos(pos);
                break;
            }
            watched.add(pos);
            found = at(world, pos, facing.getOpposite());
            if (found != null || blocks(world, pos)) {
                break;
            }
        }
        PathWatcher.INSTANCE.watch(this, world, watched, this.former.getColorSource(),
                unloaded == null ? Collections.emptyList() : Collections.singletonList(unloaded));

        final IGridNode otherNode = found == null ? null : found.former.getFormerNode();
        if (otherNode == null || found.partner != null && found.partner != this
                || !this.former.getFormerColor().matches(found.former.getFormerColor())) {
            this.disconnect();
            return;
        }

        if (found == this.partner && this.connection != null && node.getConnections().contains(this.connection)) {
            (this.owner ? this : found).measure(watched.size() - 1);
            return;
        }

        this.disconnect();
        found.disconnect();
        final IGridConnection made;
        try {
            made = AEApi.instance().grid().createGridConnection(node, otherNode);
        } catch (FailedConnectionException e) {
            BeamFormers.log.debug("A beam from {} could not join {}: {}", this.former.getFormerPos(),
                    found.former.getFormerPos(), e.getMessage());
            return;
        }

        this.partner = found;
        found.partner = this;
        this.connection = made;
        found.connection = made;
        this.owner = true;
        found.owner = false;
        this.hidden |= found.hidden;
        found.hidden = this.hidden;
        this.measure(watched.size() - 1);
        this.former.beamChanged();
        found.former.beamChanged();
    }

    /** The owner's record of what its clients draw; a change on either end sends it again. */
    private void measure(int blocksBetween) {
        final AEColor color = this.beamColor();
        if (this.blocksBetween != blocksBetween || this.color != color) {
            this.blocksBetween = blocksBetween;
            this.color = color;
            this.former.beamChanged();
        }
    }

    /** Drops the connection on both ends; the partner is woken to look again. */
    public void disconnect() {
        final BeamEnd old = this.partner;
        if (this.connection == null && old == null) {
            return;
        }
        if (this.connection != null) {
            this.connection.destroy();
        }
        this.connection = null;
        this.partner = null;
        this.owner = false;
        this.former.beamChanged();

        if (old != null && old.partner == this) {
            old.partner = null;
            old.connection = null;
            old.owner = false;
            old.former.beamChanged();
            old.former.wake();
        }
    }

    /** Shows or hides the beam on both ends, and says whether it is hidden now. */
    public boolean toggleHidden() {
        this.hidden = !this.hidden;
        this.former.beamChanged();
        if (this.partner != null) {
            this.partner.hidden = this.hidden;
            this.partner.former.beamChanged();
        }
        return this.hidden;
    }

    /** The former is leaving the world, on either side. */
    public void remove() {
        PathWatcher.INSTANCE.unwatch(this);
        this.disconnect();
        this.clientLength = 0;
        BeamRegistry.remove(this);
    }

    private AEColor beamColor() {
        final AEColor mine = this.former.getFormerColor();
        if (mine != AEColor.TRANSPARENT || this.partner == null) {
            return mine;
        }
        return this.partner.former.getFormerColor();
    }

    public void writeToStream(ByteBuf data) {
        data.writeBoolean(this.hidden);
        final BeamEnd other = this.owner && this.former.isFormerPowered() ? this.partner : null;
        data.writeFloat(other == null ? 0
                : this.blocksBetween + 1 - this.former.getTipOffset() - other.former.getTipOffset());
        data.writeByte(this.color.ordinal());
    }

    /** Returns whether anything the client draws changed. */
    public boolean readFromStream(ByteBuf data) {
        final boolean wasHidden = this.hidden;
        final float oldLength = this.clientLength;
        final AEColor oldColor = this.clientColor;
        this.hidden = data.readBoolean();
        this.clientLength = data.readFloat();
        this.clientColor = AEColor.values()[data.readByte()];
        BeamRegistry.update(this, this.clientLength > 0 && !this.hidden);
        return wasHidden != this.hidden || oldLength != this.clientLength || oldColor != this.clientColor;
    }

    @Override
    public void wake() {
        this.former.wake();
    }

    @Override
    public World getSourceWorld() {
        return this.former.getFormerWorld();
    }

    @Override
    public boolean isSourceInWorld() {
        return this.former.isFormerInWorld();
    }

    @Override
    public void addBeams(List<Beam> out) {
        final Vec3d dir = new Vec3d(this.former.getFacing().getDirectionVec());
        final Vec3d start = new Vec3d(this.former.getFormerPos()).add(0.5, 0.5, 0.5)
                .add(dir.scale(this.former.getTipOffset() - SUNK));
        out.add(new Beam(start, start.add(dir.scale(this.clientLength + 2 * SUNK)), this.former.getBeamRadius(),
                this.former.getGlowRadius(), this.clientColor, SUNK));
    }

    public void writeToNBT(NBTTagCompound data) {
        if (this.hidden) {
            data.setBoolean("hideBeam", true);
        }
    }

    public void readFromNBT(NBTTagCompound data) {
        this.hidden = data.getBoolean("hideBeam");
    }

    /** The end at a position that faces the given side, if there is one. */
    @Nullable
    static BeamEnd at(World world, BlockPos pos, EnumFacing side) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost) {
            final IPart part = ((IPartHost) te).getPart(side);
            if (part instanceof IBeamFormer) {
                return ((IBeamFormer) part).getBeamEnd();
            }
        }
        if (te instanceof IBeamFormer && ((IBeamFormer) te).getFacing() == side) {
            return ((IBeamFormer) te).getBeamEnd();
        }
        return null;
    }

    /** A beam goes through whatever light goes through, but never through a cable bus, a former or a tower. */
    public static boolean blocks(World world, BlockPos pos) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost || te instanceof IBeamFormer || te instanceof IBeamSource) {
            return true;
        }
        final IBlockState state = world.getBlockState(pos);
        return state.getMaterial().isOpaque() && state.getLightOpacity(world, pos) >= 255;
    }

}
