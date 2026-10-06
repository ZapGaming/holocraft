package fail.holocraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * systems: flat_view. A sprite drawn the way HoloCure draws it from above: lying on the ground plane, top of the
 * sprite to the north, at the stage's own pixel scale. Each sprite is lifted a hair for every block further south it
 * stands, so whatever is lower on screen lands in front, which is HoloCure's draw order.
 */
public final class FlatView {
    /** Lift per block of southward position, and the window it covers around the camera. */
    private static final float LIFT_PER_BLOCK = 0.004f, LIFT_BASE = 0.03f, LIFT_WINDOW = 64f;

    /** `worldZ` is where the sprite's feet stand; the matrix origin must already be there. */
    public static void draw(MatrixStack m, VertexConsumerProvider vcp, String sprite, int frame, float size, boolean flip,
                            double worldZ, int light, int overlay) {
        HoloSprites.Info info = HoloSprites.info(sprite);
        if (info == null) return;
        float s = size / ViewMode.mapPxPerBlock();
        float w = info.w() * s, h = info.h() * s;
        float ox = info.ox() * s, oy = info.oy() * s;
        double camZ = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().z;
        float rel = (float) Math.max(-LIFT_WINDOW, Math.min(LIFT_WINDOW, worldZ - camZ));
        float lift = LIFT_BASE + (rel + LIFT_WINDOW) * LIFT_PER_BLOCK;
        float x0 = -ox, x1 = w - ox, zTop = -oy, zBottom = h - oy;
        float u0 = flip ? 1 : 0, u1 = flip ? 0 : 1;
        MatrixStack.Entry e = m.peek();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(HoloSprites.texture(sprite, frame)));
        v(vc, e, x0, lift, zBottom, u0, 1, light, overlay);
        v(vc, e, x1, lift, zBottom, u1, 1, light, overlay);
        v(vc, e, x1, lift, zTop, u1, 0, light, overlay);
        v(vc, e, x0, lift, zTop, u0, 0, light, overlay);
    }

    private static void v(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float t, int light, int overlay) {
        vc.vertex(e, x, y, z).color(0xFFFFFFFF).texture(u, t).overlay(overlay).light(light).normal(e, 0, 1, 0);
    }

    public static int noOverlay() { return OverlayTexture.DEFAULT_UV; }

    private FlatView() {}
}
