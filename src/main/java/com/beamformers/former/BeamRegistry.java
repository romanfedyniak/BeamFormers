package com.beamformers.former;

import java.util.HashSet;
import java.util.Set;

/** The client's sources of beams to draw. */
public final class BeamRegistry {

    private static final Set<IBeamSource> DRAWN = new HashSet<>();

    private BeamRegistry() {}

    public static void update(IBeamSource source, boolean drawn) {
        if (drawn) {
            DRAWN.add(source);
        } else {
            DRAWN.remove(source);
        }
    }

    public static void remove(IBeamSource source) {
        DRAWN.remove(source);
    }

    /** The renderer drops from this whatever it finds is no longer in the world. */
    public static Set<IBeamSource> drawn() {
        return DRAWN;
    }

}
