package fail.holocraft.mixin;

import fail.holocraft.client.TopDownCamera;
import fail.holocraft.client.ViewMode;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: topdown_camera. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(Vec3d pos);

    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean holocraft$thirdPerson(boolean thirdPerson) {
        return thirdPerson || ViewMode.flat();
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void holocraft$topDown(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!ViewMode.flat() || focused == null) return;
        setPos(focused.getLerpedPos(tickDelta).add(0, TopDownCamera.HEIGHT, 0));
        setRotation(180f, 90f);
    }
}
