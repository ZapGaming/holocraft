package fail.holocraft.mixin;

import fail.holocraft.client.ViewMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * systems: ortho_projection. In the HoloCure view the world is projected flat, showing HoloCure's own 640x360 screen
 * of pixels at the stage's pixels-per-block, widened to the window's aspect; no view bobbing, no hurt tilt.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererProjectionMixin {
    private static final float HOLOCURE_SCREEN_H = 360f;

    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void holocraft$ortho(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!ViewMode.flat()) return;
        var win = MinecraftClient.getInstance().getWindow();
        float aspect = (float) win.getFramebufferWidth() / Math.max(1, win.getFramebufferHeight());
        float hh = HOLOCURE_SCREEN_H / 2f / ViewMode.mapPxPerBlock(), hw = hh * aspect;
        cir.setReturnValue(new Matrix4f().setOrtho(-hw, hw, -hh, hh, 0.05f, 512f));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void holocraft$noOutline(CallbackInfoReturnable<Boolean> cir) {
        if (ViewMode.flat()) cir.setReturnValue(false);
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void holocraft$noBob(MatrixStack m, float tickDelta, CallbackInfo ci) {
        if (ViewMode.flat()) ci.cancel();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void holocraft$noTilt(MatrixStack m, float tickDelta, CallbackInfo ci) {
        if (ViewMode.flat()) ci.cancel();
    }
}
