package fail.holocraft.mixin;

import fail.holocraft.HoloCraft;
import fail.holocraft.client.MissingHoloCureScreen;
import fail.holocraft.client.TitleBackground;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: title_background (title screen), version_label, missing_holocure_screen. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow private float backgroundAlpha;

    protected TitleScreenMixin(Text title) { super(title); }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void holocraft$background(DrawContext ctx, float delta, CallbackInfo ci) {
        if (TitleBackground.render(ctx, width, height, backgroundAlpha)) ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"), index = 1)
    private String holocraft$version(String original) {
        return "HoloCraft " + HoloCraft.VERSION + " - Minecraft " + SharedConstants.getGameVersion().getName();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void holocraft$missing(CallbackInfo ci) {
        MissingHoloCureScreen.maybeShow((TitleScreen) (Object) this);
    }
}
