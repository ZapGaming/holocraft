package fail.holocraft.mixin;

import fail.holocraft.client.ViewMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: flat_view. HoloCure has no crosshair; idols aim themselves. */
@Mixin(InGameHud.class)
public abstract class HudCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void holocraft$noCrosshair(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
        if (ViewMode.flat()) ci.cancel();
    }
}
