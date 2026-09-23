package com.beamformers.part;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ChannelTiers;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.me.GridAccessException;
import appeng.parts.PartBasicState;
import appeng.parts.PartModel;
import appeng.util.Platform;

import com.beamformers.BeamFormers;
import com.beamformers.former.BeamEnd;
import com.beamformers.former.IBeamFormer;

/** A beam former on a cable bus: joins the first former facing it along its line, at a normal cable's 8 channels. */
public class PartSmallBeamFormer extends PartBasicState implements IGridTickable, IBeamFormer {

    public static final PartModel MODEL = new PartModel(BeamFormers.id("part/small_beam_former"));

    private final BeamEnd beam = new BeamEnd(this);

    public PartSmallBeamFormer(ItemStack is) {
        super(is);
        this.getProxy().setFlags();
        this.getProxy().setChannelTier(ChannelTiers.NORMAL);
        this.getProxy().setIdlePowerUsage(0.5);
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return MODEL;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 10, 11, 11, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 6;
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.wake();
    }

    @Override
    public void removeFromWorld() {
        this.beam.remove();
        super.removeFromWorld();
    }

    @Override
    public void gridChanged() {
        this.wake();
    }

    @Override
    public boolean onPartShiftActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (!player.getHeldItem(hand).isEmpty()) {
            return false;
        }
        if (Platform.isServer()) {
            final boolean hidden = this.beam.toggleHidden();
            player.sendStatusMessage(new TextComponentTranslation(
                    hidden ? "chat.beamformers.beam_hidden" : "chat.beamformers.beam_shown"), true);
        }
        return true;
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
    public void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        this.beam.writeToStream(data);
    }

    @Override
    public boolean readFromStream(ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        return this.beam.readFromStream(data) || changed;
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.beam.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.beam.readFromNBT(data);
    }

    @Override
    public World getFormerWorld() {
        return this.getTile().getWorld();
    }

    @Override
    public BlockPos getFormerPos() {
        return this.getTile().getPos();
    }

    @Override
    public EnumFacing getFacing() {
        return this.getSide().getFacing();
    }

    @Nullable
    @Override
    public IGridNode getFormerNode() {
        return this.getProxy().isReady() ? this.getProxy().getNode() : null;
    }

    @Override
    public AEColor getFormerColor() {
        return this.getHost().getColor();
    }

    @Override
    public BlockPos getColorSource() {
        return this.getTile().getPos();
    }

    @Override
    public boolean isFormerPowered() {
        return this.isPowered();
    }

    @Override
    public float getTipOffset() {
        return 9 / 16f;
    }

    @Override
    public float getBeamRadius() {
        return 1 / 32f;
    }

    @Override
    public boolean isFormerInWorld() {
        return this.getTile() != null && !this.getTile().isInvalid() && this.getHost() != null
                && this.getHost().getPart(this.getSide()) == this;
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
        if (this.getHost() != null) {
            this.getHost().markForUpdate();
            this.getHost().markForSave();
        }
    }

}
