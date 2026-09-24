package com.beamformers.tile;

import java.io.IOException;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.networking.IGridNode;
import appeng.api.util.AEColor;

import com.beamformers.former.BeamEnd;
import com.beamformers.former.IBeamFormer;

/** A beam former of its own, mounted on whatever it was placed against and joined to the network through its base. */
public class TileDenseBeamFormer extends TileMounted implements IBeamFormer {

    private final BeamEnd beam = new BeamEnd(this);

    public TileDenseBeamFormer() {
        super(12, 10);
        this.getProxy().setIdlePowerUsage(0.5);
    }

    @Override
    protected void update() {
        this.beam.update();
    }

    @Override
    public boolean toggleHidden() {
        return this.beam.toggleHidden();
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
        this.beam.readFromNBT(data);
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
        return this.getReadyNode();
    }

    @Override
    public AEColor getFormerColor() {
        return this.getMountedColor();
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
        return this.isInWorld();
    }

    @Override
    public BeamEnd getBeamEnd() {
        return this.beam;
    }

}
