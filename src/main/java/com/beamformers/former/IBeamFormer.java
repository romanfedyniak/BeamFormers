package com.beamformers.former;

import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.networking.IGridNode;
import appeng.api.util.AEColor;

/** Whatever carries one end of a beam: a part on a cable bus or a block of its own. */
public interface IBeamFormer {

    World getFormerWorld();

    BlockPos getFormerPos();

    /** The way the beam leaves this former. */
    EnumFacing getFacing();

    /** Null until the node is part of a grid. */
    @Nullable
    IGridNode getFormerNode();

    AEColor getFormerColor();

    /** The block the colour is read from; painting its cable replaces the cable without touching the grid. */
    BlockPos getColorSource();

    /** Server side: whether the network has power, for a beam is only drawn while it does. */
    boolean isFormerPowered();

    /** How far the tip of the emitter stands from the middle of the block along the facing, in blocks. */
    float getTipOffset();

    /** Half the width of the beam's bright core, in blocks. */
    float getBeamRadius();

    /** Half the width of the faint glow around the core, in blocks. */
    float getGlowRadius();

    /** Client side: whether this former is still the one in the world, so its beam may be drawn. */
    boolean isFormerInWorld();

    BeamEnd getBeamEnd();

    /** Asks for a look along the beam on the next grid tick. */
    void wake();

    /** The beam changed: send it to the clients and save it. */
    void beamChanged();

}
