package com.beamformers.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.api.util.AEColor;

import com.beamformers.former.Beam;
import com.beamformers.former.BeamRegistry;
import com.beamformers.former.IBeamSource;

/**
 * Draws every beam once a frame: through GTCEu's bloom when it is there and drew them, otherwise after the
 * world. A tile entity renderer would stop drawing a beam as soon as the block it starts from left the screen.
 */
public final class BeamRenderer {

    public static final BeamRenderer INSTANCE = new BeamRenderer();

    private static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final Vec3d EAST = new Vec3d(1, 0, 0);

    /** Light can only brighten what is behind it, so a black beam is drawn as a dark shade instead, never bloomed. */
    static final Predicate<AEColor> BLOOMED = color -> color != AEColor.BLACK;

    private final boolean bloom = bloomPresent();

    private BeamRenderer() {}

    static boolean bloomPresent() {
        try {
            return Class.forName("gregtech.client.utils.IBloomEffect").isInterface();
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            BeamRegistry.drawn().clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Predicate<AEColor> which = color -> true;
        if (this.bloom) {
            BloomBeams.register();
            if (BloomBeams.takeDrawn()) {
                which = BLOOMED.negate();
            }
        }
        if (BeamRegistry.drawn().isEmpty()) {
            return;
        }
        final Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view == null) {
            return;
        }
        final float pt = event.getPartialTicks();

        final double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        final double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        final double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        final float lastX = OpenGlHelper.lastBrightnessX;
        final float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        draw(cx, cy, cz, which.and(BLOOMED));
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        draw(cx, cy, cz, which.and(BLOOMED.negate()));

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    private static void draw(double cx, double cy, double cz, Predicate<AEColor> which) {
        final Tessellator tessellator = Tessellator.getInstance();
        tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addBeams(tessellator.getBuffer(), cx, cy, cz, which);
        tessellator.draw();
    }

    /** Adds every visible beam of the given colours to a buffer of coloured quads, relative to the camera. */
    static void addBeams(BufferBuilder buffer, double cx, double cy, double cz, Predicate<AEColor> which) {
        final Minecraft mc = Minecraft.getMinecraft();
        final Frustum frustum = new Frustum();
        frustum.setPosition(cx, cy, cz);
        final Vec3d camera = new Vec3d(cx, cy, cz);
        final List<Beam> beams = new ArrayList<>();
        for (Iterator<IBeamSource> it = BeamRegistry.drawn().iterator(); it.hasNext(); ) {
            final IBeamSource source = it.next();
            if (source.getSourceWorld() != mc.world || !source.isSourceInWorld()) {
                it.remove();
                continue;
            }
            source.addBeams(beams);
        }
        for (Beam beam : beams) {
            if (!which.test(beam.color)
                    || !frustum.isBoundingBoxInFrustum(new AxisAlignedBB(beam.start, beam.end).grow(beam.glowRadius))) {
                continue;
            }
            final Vec3d start = beam.start.subtract(camera);
            final Vec3d end = beam.end.subtract(camera);
            final boolean dark = !BLOOMED.test(beam.color);
            sides(buffer, start, end, beam.radius, dark ? beam.color.mediumVariant : beam.color.whiteVariant, 0.9f);
            final Vec3d inset = end.subtract(start).normalize().scale(beam.sunk);
            sides(buffer, start.add(inset), end.subtract(inset), beam.glowRadius,
                    dark ? beam.color.blackVariant : beam.color.mediumVariant, 0.3f);
        }
    }

    /** The four long sides only: an end cap would lie in the plane of the emitter's tip and flicker against it. */
    private static void sides(BufferBuilder b, Vec3d start, Vec3d end, float radius, int rgb, float alpha) {
        final Vec3d dir = end.subtract(start).normalize();
        final Vec3d across = dir.crossProduct(Math.abs(dir.y) < 0.99 ? UP : EAST).normalize();
        final Vec3d u = across.scale(radius);
        final Vec3d v = dir.crossProduct(across).scale(radius);
        final Vec3d[] corners = { u.add(v), u.subtract(v), u.scale(-1).subtract(v), v.subtract(u) };
        final int r = rgb >> 16 & 0xFF;
        final int g = rgb >> 8 & 0xFF;
        final int bl = rgb & 0xFF;
        final int a = (int) (alpha * 255);
        for (int i = 0; i < 4; i++) {
            final Vec3d c0 = corners[i];
            final Vec3d c1 = corners[(i + 1) % 4];
            vertex(b, start.add(c0), r, g, bl, a);
            vertex(b, end.add(c0), r, g, bl, a);
            vertex(b, end.add(c1), r, g, bl, a);
            vertex(b, start.add(c1), r, g, bl, a);
        }
    }

    private static void vertex(BufferBuilder b, Vec3d p, int r, int g, int bl, int a) {
        b.pos(p.x, p.y, p.z).color(r, g, bl, a).endVertex();
    }

}
