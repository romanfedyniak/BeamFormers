package com.beamformers.tile;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.pathing.ChannelTiers;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.helpers.ICustomCollision;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTile;

import com.beamformers.former.BeamEnd;
import com.beamformers.former.IBeamFormer;

/** A beam former of its own, mounted on whatever it was placed against and joined to the network through its base. */
public class TileDenseBeamFormer extends AENetworkTile implements IGridTickable, IBeamFormer, ICustomCollision {

    private final BeamEnd beam = new BeamEnd(this);

    public TileDenseBeamFormer() {
        this.getProxy().setFlags();
        this.getProxy().setChannelTier(ChannelTiers.DENSE);
        this.getProxy().setIdlePowerUsage(0.5);
        this.getProxy().setValidSides(EnumSet.noneOf(EnumFacing.class));
    }

    @Override
    public void onPlacement(ItemStack stack, EntityPlayer player, EnumFacing side) {
        super.onPlacement(stack, player, side);
        this.setOrientation(side, side.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : EnumFacing.UP);
    }

    @Override
    public void setOrientation(EnumFacing forward, EnumFacing up) {
        super.setOrientation(forward, up);
        this.updateSides();
        this.wake();
    }

    private void updateSides() {
        this.getProxy().setValidSides(EnumSet.of(this.getForward().getOpposite()));
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void gridChanged() {
        this.wake();
    }

    @MENetworkEventSubscribe
    public void powerChanged(MENetworkPowerStatusChange event) {
        this.markForUpdate();
    }

    @Override
    public void onChunkUnload() {
        this.beam.remove();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        this.beam.remove();
        super.invalidate();
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(20, 20, false, true);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull IGridNode node, int ticksSinceLastCall) {
        if (!this.getProxy().isReady()) {
            return TickRateModulation.SAME;
        }
        this.beam.update();
        return TickRateModulation.SLEEP;
    }

    @Override
    protected void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        this.beam.writeToStream(data);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        return this.beam.readFromStream(data) || changed;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.beam.writeToNBT(data);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.updateSides();
        this.beam.readFromNBT(data);
    }

    @Override
    public Iterable<AxisAlignedBB> getSelectedBoundingBoxesFromPool(World w, BlockPos pos, Entity thePlayer, boolean b) {
        return Collections.singletonList(this.box());
    }

    @Override
    public void addCollidingBlockToList(World w, BlockPos pos, AxisAlignedBB bb, List<AxisAlignedBB> out, Entity e) {
        out.add(this.box());
    }

    /** The body reaches 12 pixels out of its base and is 10 across. */
    private AxisAlignedBB box() {
        final EnumFacing facing = this.getForward();
        final double near = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 0 : 4 / 16d;
        final double far = near + 12 / 16d;
        final double lo = 3 / 16d;
        final double hi = 13 / 16d;
        switch (facing.getAxis()) {
            case X:
                return new AxisAlignedBB(near, lo, lo, far, hi, hi);
            case Y:
                return new AxisAlignedBB(lo, near, lo, hi, far, hi);
            default:
                return new AxisAlignedBB(lo, lo, near, hi, hi, far);
        }
    }

    public boolean toggleHidden() {
        return this.beam.toggleHidden();
    }

    @Override
    public World getFormerWorld() {
        return this.world;
    }

    @Override
    public BlockPos getFormerPos() {
        return this.pos;
    }

    @Override
    public EnumFacing getFacing() {
        return this.getForward();
    }

    @Nullable
    @Override
    public IGridNode getFormerNode() {
        return this.getProxy().isReady() ? this.getProxy().getNode() : null;
    }

    /** The colour of the cable bus behind the base. */
    @Override
    public AEColor getFormerColor() {
        final TileEntity te = this.world.getTileEntity(this.getColorSource());
        return te instanceof IPartHost ? ((IPartHost) te).getColor() : AEColor.TRANSPARENT;
    }

    @Override
    public BlockPos getColorSource() {
        return this.pos.offset(this.getForward().getOpposite());
    }

    @Override
    public boolean isFormerPowered() {
        return this.getProxy().isPowered();
    }

    @Override
    public float getTipOffset() {
        return 4 / 16f;
    }

    /** Just inside the 3 pixel tip, so the sides of the beam's sunk end do not lie in the tip's. */
    @Override
    public float getBeamRadius() {
        return 1.4f / 16;
    }

    @Override
    public float getGlowRadius() {
        return 5 / 32f;
    }

    @Override
    public boolean isFormerInWorld() {
        return !this.isInvalid() && this.world.getTileEntity(this.pos) == this;
    }

    @Override
    public BeamEnd getBeamEnd() {
        return this.beam;
    }

    @Override
    public void wake() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (GridAccessException e) {
            // not in a grid yet; the first tick after it joins looks anyway
        }
    }

    @Override
    public void beamChanged() {
        this.markForUpdate();
        this.saveChanges();
    }

}
