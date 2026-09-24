package com.beamformers.former;

import net.minecraft.util.math.Vec3d;

import appeng.api.util.AEColor;

/** One beam as a client draws it, in world coordinates. */
public final class Beam {

    public final Vec3d start;
    public final Vec3d end;
    public final float radius;
    public final float glowRadius;
    public final AEColor color;
    /** How far each end reaches into its emitter; the glow, wider than the tip, stops short of it by as much. */
    public final double sunk;

    public Beam(Vec3d start, Vec3d end, float radius, float glowRadius, AEColor color, double sunk) {
        this.start = start;
        this.end = end;
        this.radius = radius;
        this.glowRadius = glowRadius;
        this.color = color;
        this.sunk = sunk;
    }

}
