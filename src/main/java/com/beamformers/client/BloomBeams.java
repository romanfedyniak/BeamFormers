package com.beamformers.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import gregtech.client.renderer.IRenderSetup;
import gregtech.client.shader.postprocessing.BloomType;
import gregtech.client.utils.BloomEffectUtil;
import gregtech.client.utils.EffectRenderContext;
import gregtech.client.utils.IBloomEffect;

import com.beamformers.former.BeamRegistry;

/**
 * The beams drawn through GTCEu's bloom, which Lumenized carries on its own. Only loaded once that bloom is
 * known to be there.
 */
final class BloomBeams implements IBloomEffect, IRenderSetup {

    private static final BloomBeams INSTANCE = new BloomBeams();

    @Nullable
    private BloomEffectUtil.BloomRenderTicket ticket;
    private boolean drewThisFrame;
    private float lastBrightnessX;
    private float lastBrightnessY;

    private BloomBeams() {}

    /**
     * Holds a place in the bloom pass only while there are beams to draw; none is given while an OptiFine shader
     * pack is on. A place in it costs a blur over the whole screen every frame, beams or not.
     */
    static void register() {
        final boolean beams = !BeamRegistry.drawn().isEmpty();
        final BloomEffectUtil.BloomRenderTicket held = INSTANCE.ticket;
        if (held != null && held.isValid()) {
            if (!beams) {
                held.invalidate();
                INSTANCE.ticket = null;
            }
        } else if (beams) {
            INSTANCE.ticket = BloomEffectUtil.registerBloomRender(INSTANCE, BloomType.UNITY, INSTANCE, null);
        }
    }

    /** Whether the bloom pass has drawn the beams since the last call. */
    static boolean takeDrawn() {
        final boolean drew = INSTANCE.drewThisFrame;
        INSTANCE.drewThisFrame = false;
        return drew;
    }

    @Override
    public boolean shouldRenderBloomEffect(@Nonnull EffectRenderContext context) {
        return !BeamRegistry.drawn().isEmpty();
    }

    @Override
    public void renderBloomEffect(@Nonnull BufferBuilder buffer, @Nonnull EffectRenderContext context) {
        BeamRenderer.addBeams(buffer, context.cameraX(), context.cameraY(), context.cameraZ(), BeamRenderer.BLOOMED);
        this.drewThisFrame = true;
    }

    @Override
    public void preDraw(@Nonnull BufferBuilder buffer) {
        this.lastBrightnessX = OpenGlHelper.lastBrightnessX;
        this.lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    }

    @Override
    public void postDraw(@Nonnull BufferBuilder buffer) {
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, this.lastBrightnessX, this.lastBrightnessY);
    }

}
