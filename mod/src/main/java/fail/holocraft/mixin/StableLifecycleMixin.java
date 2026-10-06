package fail.holocraft.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** systems: stage_world_prompt. Reopening a stage world must not ask to make a backup of "experimental settings". */
@Mixin(LevelProperties.class)
public abstract class StableLifecycleMixin {
    @Inject(method = "getLifecycle", at = @At("RETURN"), cancellable = true)
    private void holocraft$stable(CallbackInfoReturnable<Lifecycle> cir) {
        cir.setReturnValue(Lifecycle.stable());
    }
}
