package fail.holocraft.mixin;

import fail.holocraft.HoloCraft;
import fail.holocraft.client.HoloSprites;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: logo. The HoloCraft logo, lettered in HoloCure's title font, replaces Minecraft's. */
@Mixin(LogoDrawer.class)
public abstract class LogoDrawerMixin {
    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("HEAD"), cancellable = true)
    private void holocraft$logo(DrawContext ctx, int screenWidth, float alpha, int y, CallbackInfo ci) {
        HoloSprites.Info logo = HoloSprites.info("_logo");
        if (logo == null) return;
        int w = 256, h = Math.round(256f * logo.h() / logo.w());
        ctx.drawTexture(RenderLayer::getGuiTextured, HoloCraft.id("textures/gui/logo.png"), screenWidth / 2 - w / 2, y - 6, 0, 0,
                w, h, logo.w(), logo.h(), logo.w(), logo.h(), ColorHelper.getWhite(alpha));
        ci.cancel();
    }
}
