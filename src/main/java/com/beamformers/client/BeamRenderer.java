package com.beamformers.client;

import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.api.util.AEColor;

import com.beamformers.former.BeamEnd;
import com.beamformers.former.BeamRegistry;
import com.beamformers.former.IBeamFormer;

/**
 * Draws every beam once a frame: through GTCEu's bloom when it is there and drew them, otherwise after the
 * world. A tile entity renderer would stop drawing a beam as soon as the block it starts from left the screen.
 */
public final class BeamRenderer {

    public static final BeamRenderer INSTANCE = new BeamRenderer();

    /** How far each end of a beam reaches into its former, so no gap shows at the tip. */
    private static final double SUNK = 1 / 32d;

    private final boolean bloom = bloomPresent();

    private BeamRenderer() {}

    private static boolean bloomPresent() {
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
        if (this.bloom) {
            BloomBeams.register();
            if (BloomBeams.takeDrawn()) {
                return;
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

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        final float lastX = OpenGlHelper.lastBrightnessX;
        final float lastY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addBeams(buffer, view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        tessellator.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    /** Adds every visible beam to a buffer of coloured quads, relative to the camera. */
    static void addBeams(BufferBuilder buffer, double cx, double cy, double cz) {
        final Minecraft mc = Minecraft.getMinecraft();
        final Frustum frustum = new Frustum();
        frustum.setPosition(cx, cy, cz);
        for (Iterator<BeamEnd> it = BeamRegistry.drawn().iterator(); it.hasNext(); ) {
            final BeamEnd end = it.next();
            final IBeamFormer former = end.getFormer();
            if (former.getFormerWorld() != mc.world || !former.isFormerInWorld()) {
                it.remove();
                continue;
            }
            final AxisAlignedBB core = beamBox(former, end.getClientLength(), former.getBeamRadius());
            if (!frustum.isBoundingBoxInFrustum(core.grow(0.5))) {
                continue;
            }
            final AEColor color = end.getClientColor();
            final EnumFacing.Axis axis = former.getFacing().getAxis();
            sides(buffer, core.offset(-cx, -cy, -cz), axis, color.whiteVariant, 0.9f);
            sides(buffer, beamBox(former, end.getClientLength(), former.getGlowRadius()).offset(-cx, -cy, -cz),
                    axis, color.mediumVariant, 0.3f);
        }
    }

    private static AxisAlignedBB beamBox(IBeamFormer former, float length, float radius) {
        final BlockPos pos = former.getFormerPos();
        final EnumFacing facing = former.getFacing();
        final Vec3d dir = new Vec3d(facing.getDirectionVec());
        final Vec3d start = new Vec3d(pos).add(0.5, 0.5, 0.5).add(dir.scale(former.getTipOffset() - SUNK));
        final Vec3d end = start.add(dir.scale(length + 2 * SUNK));
        final double rx = facing.getAxis() == EnumFacing.Axis.X ? 0 : radius;
        final double ry = facing.getAxis() == EnumFacing.Axis.Y ? 0 : radius;
        final double rz = facing.getAxis() == EnumFacing.Axis.Z ? 0 : radius;
        return new AxisAlignedBB(start, end).grow(rx, ry, rz);
    }

    /** The four long sides only: an end cap would lie in the plane of the emitter's tip and flicker against it. */
    private static void sides(BufferBuilder b, AxisAlignedBB bb, EnumFacing.Axis axis, int rgb, float alpha) {
        final int r = rgb >> 16 & 0xFF;
        final int g = rgb >> 8 & 0xFF;
        final int bl = rgb & 0xFF;
        final int a = (int) (alpha * 255);
        final double x0 = bb.minX, y0 = bb.minY, z0 = bb.minZ, x1 = bb.maxX, y1 = bb.maxY, z1 = bb.maxZ;
        if (axis != EnumFacing.Axis.Z) {
            quad(b, r, g, bl, a, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
            quad(b, r, g, bl, a, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        }
        if (axis != EnumFacing.Axis.X) {
            quad(b, r, g, bl, a, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
            quad(b, r, g, bl, a, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        }
        if (axis != EnumFacing.Axis.Y) {
            quad(b, r, g, bl, a, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
            quad(b, r, g, bl, a, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        }
    }

    private static void quad(BufferBuilder b, int r, int g, int bl, int a, double... p) {
        for (int i = 0; i < 12; i += 3) {
            b.pos(p[i], p[i + 1], p[i + 2]).color(r, g, bl, a).endVertex();
        }
    }

}
