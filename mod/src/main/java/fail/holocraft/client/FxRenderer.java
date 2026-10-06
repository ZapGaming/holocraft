package fail.holocraft.client;

import fail.holocraft.net.Payloads;
import fail.holocraft.sheet.IdolsRow;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * systems: fx_renderer. Attack visuals from Fx packets: shots fly from the idol to the target, slashes and thrusts
 * lie flat on the ground the way HoloCure draws them from above, special icons rise and fade.
 */
public final class FxRenderer {
    static final class Live {
        final Payloads.Fx fx;
        final String kind;
        int age;
        Live(Payloads.Fx fx, String kind) { this.fx = fx; this.kind = kind; }
    }

    private static final List<Live> LIVE = new ArrayList<>();
    private static final boolean DEBUG = Boolean.getBoolean("holocraft.debugFx");
    private static int received, rendered;

    public static void add(Payloads.Fx fx) {
        String kind = "icon";
        for (IdolsRow r : IdolsRow.ALL)
            if (r.fxSprite().equals(fx.sprite())) kind = r.attack().equals("projectile_burst") ? "shot" : "flat";
        if (fx.length() <= 0) kind = "icon";
        synchronized (LIVE) { LIVE.add(new Live(fx, kind)); if (LIVE.size() > 400) LIVE.remove(0); }
        if (DEBUG && ++received % 20 == 1) fail.holocraft.HoloCraft.LOG.info("fx {} {} at {},{},{} (live {})", kind, fx.sprite(), fx.x(), fx.y(), fx.z(), LIVE.size());
    }

    public static void tick() {
        synchronized (LIVE) {
            for (Live l : LIVE) l.age++;
            LIVE.removeIf(l -> l.age > l.fx.life());
        }
    }

    public static void render(WorldRenderContext ctx) {
        List<Live> copy;
        synchronized (LIVE) { copy = new ArrayList<>(LIVE); }
        if (DEBUG) {
            Vec3d look = Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(3);
            Vec3d at = ctx.camera().getPos().add(look);
            copy.add(new Live(new Payloads.Fx("spr_Ame_portrait", at.x, at.y, at.z, 0, 0, 1f, 1000), "icon"));
        }
        if (copy.isEmpty()) return;
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        MatrixStack m = ctx.matrixStack() != null ? ctx.matrixStack() : new MatrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (vcp == null) return;
        float delta = ctx.tickCounter().getTickDelta(false);
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        if (DEBUG && ++rendered % 200 == 1) fail.holocraft.HoloCraft.LOG.info("fx render {} live, matrixStack={}", copy.size(), ctx.matrixStack() != null);
        for (Live l : copy) {
            Payloads.Fx fx = l.fx;
            HoloSprites.Info info = HoloSprites.info(fx.sprite());
            if (info == null) continue;
            float t = (l.age + delta) / Math.max(1, fx.life());
            int frame = Math.min(info.frames() - 1, (int) (t * info.frames()));
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(HoloSprites.texture(fx.sprite(), frame)));
            m.push();
            switch (l.kind) {
                case "shot" -> {
                    double d = fx.length() * Math.min(1, t * 1.6);
                    m.translate(fx.x() + Math.cos(fx.yaw()) * d - cp.x, fx.y() - cp.y, fx.z() + Math.sin(fx.yaw()) * d - cp.z);
                    m.multiply(cam.getRotation());
                    quad(vc, m.peek(), fx.scale(), fx.scale(), light, 255);
                }
                case "flat" -> {
                    m.translate(fx.x() - cp.x, fx.y() - cp.y - 0.8, fx.z() - cp.z);
                    m.multiply(RotationAxis.NEGATIVE_Y.rotation(fx.yaw()));
                    m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                    float w = fx.length(), h = w * info.h() / (float) info.w();
                    quad(vc, m.peek(), w, Math.max(h, fx.scale()), light, 255);
                }
                default -> {
                    m.translate(fx.x() - cp.x, fx.y() - cp.y + t * 0.6, fx.z() - cp.z);
                    m.multiply(cam.getRotation());
                    float s = fx.scale();
                    quad(vc, m.peek(), s * info.w() / (float) info.h(), s, light, (int) (255 * (1 - t)));
                }
            }
            m.pop();
        }
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry e, float w, float h, int light, int alpha) {
        int c = (Math.max(0, Math.min(255, alpha)) << 24) | 0xFFFFFF;
        float x = w / 2, y = h / 2;
        vc.vertex(e, -x, -y, 0).color(c).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 1, 0);
        vc.vertex(e, x, -y, 0).color(c).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 1, 0);
        vc.vertex(e, x, y, 0).color(c).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 1, 0);
        vc.vertex(e, -x, y, 0).color(c).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0, 1, 0);
    }

    private FxRenderer() {}
}
