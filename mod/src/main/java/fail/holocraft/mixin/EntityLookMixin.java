package fail.holocraft.mixin;

import fail.holocraft.client.ViewMode;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: topdown_camera. In the HoloCure view the mouse does not turn the player. */
@Mixin(Entity.class)
public abstract class EntityLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void holocraft$noMouseLook(double dx, double dy, CallbackInfo ci) {
        if (ViewMode.flat() && (Object) this instanceof ClientPlayerEntity) ci.cancel();
    }
}
