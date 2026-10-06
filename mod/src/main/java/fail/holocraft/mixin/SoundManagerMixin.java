package fail.holocraft.mixin;

import fail.holocraft.client.HoloSfx;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** systems: sfx_player. Sounds whose HoloCure file is a WAV play through HoloSfx. */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void holocraft$play(SoundInstance sound, CallbackInfo ci) {
        if (HoloSfx.play(sound)) ci.cancel();
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void holocraft$playDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        if (HoloSfx.play(sound)) ci.cancel();
    }
}
