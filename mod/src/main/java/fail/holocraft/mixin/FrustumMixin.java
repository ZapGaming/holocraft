package fail.holocraft.mixin;

import fail.holocraft.client.ViewMode;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * systems: ortho_frustum. Vanilla backs the frustum off until a 16-block box around the camera fits inside it, which
 * an orthographic view only 11 blocks tall never does: the loop never ends. The flat view needs no backing off.
 */
@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Inject(method = "coverBoxAroundSetPosition", at = @At("HEAD"), cancellable = true)
    private void holocraft$noRecede(int boxSize, CallbackInfoReturnable<Frustum> cir) {
        if (ViewMode.flat()) cir.setReturnValue((Frustum) (Object) this);
    }
}
