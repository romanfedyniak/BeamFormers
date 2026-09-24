package com.beamformers.former;

import java.util.List;

import net.minecraft.world.World;

/** Whatever a client draws beams from: the owning end of a former pair, or a beam tower. */
public interface IBeamSource {

    World getSourceWorld();

    /** Whether this is still the one in the world, so its beams may be drawn. */
    boolean isSourceInWorld();

    void addBeams(List<Beam> out);

}
