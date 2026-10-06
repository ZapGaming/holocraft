package fail.holocraft.client;

import fail.holocraft.entity.HoloMob;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/** systems: sprite_renderer. Fans and bosses: animated thick pixel models from the Minecraft camera, flat HoloCure sprites from the top-down one. */
public class SpriteEntityRenderer extends EntityRenderer<HoloMob, SpriteEntityRenderer.State> {
    /** HoloCure's sprites are drawn at this many pixels per block, so every fan keeps its size relative to the others. */
    public static final float PX_PER_BLOCK = 24f;

    public static class State extends EntityRenderState {
        String sprite;
        int frame;
        float size;
        boolean hurt, flip;
        int depthPx;
    }

    public SpriteEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void updateRenderState(HoloMob e, State s, float tickDelta) {
        super.updateRenderState(e, s, tickDelta);
        s.sprite = e.sprite();
        s.size = e.spriteSize();
        s.depthPx = e.depthPx();
        s.frame = HoloSprites.frameAt(e.sprite(), e.age + tickDelta, 8);
        s.hurt = e.hurtTime > 0;
        Vec3d v = e.getVelocity();
        if (v.horizontalLengthSquared() < 1e-4 && e.getTarget() != null) v = e.getTarget().getPos().subtract(e.getPos());
        s.flip = Facing.flip(e, v.x, v.z);
    }

    @Override
    public void render(State s, MatrixStack m, VertexConsumerProvider vcp, int light) {
        if (HoloSprites.info(s.sprite) == null) return;
        int overlay = OverlayTexture.getUv(0, s.hurt);
        if (ViewMode.flat()) {
            FlatView.draw(m, vcp, s.sprite, s.frame, s.size, s.flip, s.z, light, overlay);
        } else {
            float yaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
            PixelModels.drawFacing(m, vcp, s.sprite, s.frame, s.size, PX_PER_BLOCK, s.depthPx, s.flip, yaw, light, overlay);
        }
        super.render(s, m, vcp, light);
    }
}
