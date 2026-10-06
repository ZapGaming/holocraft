package fail.holocraft.mixin;

import fail.holocraft.client.FlatView;
import fail.holocraft.client.HoloSprites;
import fail.holocraft.client.PixelModels;
import fail.holocraft.client.SpriteEntityRenderer;
import fail.holocraft.client.ViewMode;
import fail.holocraft.client.duck.IdolRenderState;
import fail.holocraft.item.IdolItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: player_as_idol (the drawing half; PlayerRendererMixin picks the idol). */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerIdolDrawMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void holocraft$drawIdol(LivingEntityRenderState state, MatrixStack m, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (!(state instanceof IdolRenderState idol) || idol.holocraft$sprite() == null) return;
        if (HoloSprites.info(idol.holocraft$sprite()) == null) return;
        if (state.invisible) { ci.cancel(); return; }
        int overlay = OverlayTexture.DEFAULT_UV;
        if (ViewMode.flat()) {
            FlatView.draw(m, vcp, idol.holocraft$sprite(), idol.holocraft$frame(), 1f, idol.holocraft$flip(), state.z, light, overlay);
        } else {
            float yaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
            PixelModels.drawFacing(m, vcp, idol.holocraft$sprite(), idol.holocraft$frame(), 1f, SpriteEntityRenderer.PX_PER_BLOCK,
                    idol.holocraft$depthPx(), idol.holocraft$flip(), yaw, light, overlay);
        }
        if (state.displayName != null)
            ((EntityRendererAccessor) this).holocraft$label(state, state.displayName, m, vcp, light);
        ci.cancel();
    }
}
