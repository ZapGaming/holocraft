package fail.holocraft.mixin;

import fail.holocraft.client.ViewMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: topdown_camera. No first-person hand while the camera hangs above the player. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void holocraft$noHand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        if (ViewMode.flat()) ci.cancel();
    }
}
