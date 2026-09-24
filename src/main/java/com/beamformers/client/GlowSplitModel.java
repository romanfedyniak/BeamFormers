package com.beamformers.client;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;

/**
 * Draws a block model's pink lights, the faces drawn from its {@code _glow} texture, once more in the
 * {@link GlowLayer}, so they bloom; the model's own layer keeps them too.
 */
public final class GlowSplitModel implements IBakedModel {

    private final IBakedModel parent;

    public GlowSplitModel(IBakedModel parent) {
        this.parent = parent;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        final List<BakedQuad> quads = this.parent.getQuads(state, side, rand);
        final BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
        final BlockRenderLayer glow = GlowLayer.get();
        if (layer == null || layer != glow || glow == BlockRenderLayer.CUTOUT) {
            return quads;
        }
        return quads.stream().filter(GlowSplitModel::isGlow).collect(Collectors.toList());
    }

    private static boolean isGlow(BakedQuad quad) {
        return quad.getSprite().getIconName().endsWith("_glow");
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return this.parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.parent.getParticleTexture();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.parent.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.parent.getOverrides();
    }

}
