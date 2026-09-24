package com.beamformers.client;

import net.minecraft.util.BlockRenderLayer;

import gregtech.client.utils.BloomEffectUtil;

/**
 * The layer the pink lights of a block model are drawn in: GTCEu's bloom layer, which Lumenized carries too, so
 * they glow. Without either mod, or under an OptiFine shader pack, it is the model's own cutout layer.
 */
public final class GlowLayer {

    private static final boolean BLOOM = BeamRenderer.bloomPresent();

    private GlowLayer() {}

    public static BlockRenderLayer get() {
        return BLOOM ? Bloom.layer() : BlockRenderLayer.CUTOUT;
    }

    private static final class Bloom {

        static BlockRenderLayer layer() {
            final BlockRenderLayer layer = BloomEffectUtil.getEffectiveBloomLayer();
            return layer == null ? BlockRenderLayer.CUTOUT : layer;
        }
    }

}
