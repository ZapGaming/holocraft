package fail.holocraft.mixin;

import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * systems: stage_world_prompt. HoloCure stages are data-driven world presets, which vanilla labels "experimental" and
 * asks about before creating the world. HoloCraft's own stages are the point of the game, so the question is skipped.
 */
@Mixin(IntegratedServerLoader.class)
public abstract class NoExperimentalWarningMixin {
    @ModifyVariable(method = "tryLoad", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static boolean holocraft$bypass(boolean bypassWarnings) {
        return true;
    }
}
