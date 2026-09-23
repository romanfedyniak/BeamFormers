package com.beamformers.former;

import java.util.HashSet;
import java.util.Set;

/** The beams a client draws: the owning end of every joined, powered pair it was told about. */
public final class BeamRegistry {

    private static final Set<BeamEnd> DRAWN = new HashSet<>();

    private BeamRegistry() {}

    static void update(BeamEnd end) {
        if (end.getClientLength() > 0 && !end.isHidden()) {
            DRAWN.add(end);
        } else {
            DRAWN.remove(end);
        }
    }

    static void remove(BeamEnd end) {
        DRAWN.remove(end);
    }

    /** The renderer drops from this whatever it finds is no longer in the world. */
    public static Set<BeamEnd> drawn() {
        return DRAWN;
    }

}
