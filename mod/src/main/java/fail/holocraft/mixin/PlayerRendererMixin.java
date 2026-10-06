package fail.holocraft.mixin;

import fail.holocraft.client.Facing;
import fail.holocraft.client.FlatView;
import fail.holocraft.client.HoloSprites;
import fail.holocraft.client.PixelModels;
import fail.holocraft.client.SpriteEntityRenderer;
import fail.holocraft.client.ViewMode;
import fail.holocraft.client.duck.IdolRenderState;
import fail.holocraft.item.IdolItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * systems: player_as_idol. A player holding an idol is drawn as that idol, standing or running, in both views: a thick
 * pixel model from the Minecraft camera, the flat sprite from the HoloCure one. Holding anything else, they are Steve.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void holocraft$idol(AbstractClientPlayerEntity p, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        IdolRenderState idol = (IdolRenderState) state;
        if (!(p.getMainHandStack().getItem() instanceof IdolItem item)) { idol.holocraft$set(null, 0, 0, false); return; }
        var row = item.row();
        boolean moving = p.limbAnimator.getSpeed(tickDelta) > 0.08f;
        String sprite = moving ? row.runSprite() : row.idleSprite();
        float ticks = p.age + tickDelta;
        int frame = HoloSprites.frameAt(sprite, ticks, moving ? 12 : 6);
        boolean flip = Facing.flip(p, p.getX() - p.prevX, p.getZ() - p.prevZ);
        idol.holocraft$set(sprite, frame, row.depthPx(), flip);
    }
}
