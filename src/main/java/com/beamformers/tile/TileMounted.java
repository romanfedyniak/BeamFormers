package com.beamformers.tile;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

/**
 * A block mounted on whatever it was placed against, facing away from it, joined to the network through its base
 * only and taking its colour from the cable bus there. It looks along its beams on a grid tick when woken.
 */
public abstract class TileMounted extends AENetworkTile implements IGridTickable, ICustomCollision {

    private final double depth;
    private final double halfWidth;

    /** The body's size in pixels: how far it reaches out of its base, and how wide it is. */
    protected TileMounted(int depth, int width) {
        this.depth = depth / 16d;
        this.halfWidth = width / 32d;
        this.getProxy().setFlags();
        this.getProxy().setChannelTier(ChannelTiers.DENSE);
        this.getProxy().setValidSides(EnumSet.noneOf(EnumFacing.class));
    }

    /** Server side, on a grid tick: joins, keeps or drops the beams. */
    protected abstract void update();

    /** Shows or hides the beams, and says whether they are hidden now. */
    public abstract boolean toggleHidden();

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
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.updateSides();
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
        this.update();
        return TickRateModulation.SLEEP;
    }

    @Override
    public Iterable<AxisAlignedBB> getSelectedBoundingBoxesFromPool(World w, BlockPos pos, Entity thePlayer, boolean b) {
        return Collections.singletonList(this.box());
    }

    @Override
    public void addCollidingBlockToList(World w, BlockPos pos, AxisAlignedBB bb, List<AxisAlignedBB> out, Entity e) {
        out.add(this.box());
    }

    private AxisAlignedBB box() {
        final EnumFacing facing = this.getForward();
        final double near = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 0 : 1 - this.depth;
        final double far = near + this.depth;
        final double lo = 0.5 - this.halfWidth;
        final double hi = 0.5 + this.halfWidth;
        switch (facing.getAxis()) {
            case X:
                return new AxisAlignedBB(near, lo, lo, far, hi, hi);
            case Y:
                return new AxisAlignedBB(lo, near, lo, hi, far, hi);
            default:
                return new AxisAlignedBB(lo, lo, near, hi, hi, far);
        }
    }

    @Nullable
    public IGridNode getReadyNode() {
        return this.getProxy().isReady() ? this.getProxy().getNode() : null;
    }

    /** The block behind the base, whose cable bus gives the colour. */
    public BlockPos getColorSource() {
        return this.pos.offset(this.getForward().getOpposite());
    }

    public AEColor getMountedColor() {
        final TileEntity te = this.world.getTileEntity(this.getColorSource());
        return te instanceof IPartHost ? ((IPartHost) te).getColor() : AEColor.TRANSPARENT;
    }

    public boolean isInWorld() {
        return !this.isInvalid() && this.world.getTileEntity(this.pos) == this;
    }

    public void wake() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (GridAccessException e) {
            // not in a grid yet; the first tick after it joins looks anyway
        }
    }

    /** What the clients draw changed: send it and save it. */
    public void beamChanged() {
        this.markForUpdate();
        this.saveChanges();
    }

}
