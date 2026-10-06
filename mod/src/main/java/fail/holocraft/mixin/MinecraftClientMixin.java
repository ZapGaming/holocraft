package fail.holocraft.mixin;

import fail.holocraft.client.ClientStage;
import fail.holocraft.client.HoloPack;
import fail.holocraft.registry.HcSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicInstance;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/** systems: pack_provider, window_title, music. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/resource/ResourcePackManager;<init>([Lnet/minecraft/resource/ResourcePackProvider;)V"))
    private ResourcePackProvider[] holocraft$addPack(ResourcePackProvider[] providers) {
        ResourcePackProvider[] out = Arrays.copyOf(providers, providers.length + 1);
        out[providers.length] = new HoloPack();
        return out;
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void holocraft$title(CallbackInfoReturnable<String> cir) {
        String t = cir.getReturnValue();
        int dash = t.indexOf(" - ");
        cir.setReturnValue("HoloCraft" + (dash >= 0 ? t.substring(dash) : ""));
    }

    @Inject(method = "getMusicInstance", at = @At("HEAD"), cancellable = true)
    private void holocraft$music(CallbackInfoReturnable<MusicInstance> cir) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player == null || mc.currentScreen != null && mc.currentScreen.getMusic() != null) return;
        if (!ClientStage.active) return;
        // each stage plays its own night music (stages.music, as sounds.json holocraft:music.stage_<id>)
        SoundEvent ev = HcSounds.event(ClientStage.boss ? "music.boss" : "music.stage_" + ClientStage.stage);
        if (ev == null) return;
        cir.setReturnValue(new MusicInstance(new MusicSound(Registries.SOUND_EVENT.getEntry(ev), 0, 0, true)));
    }
}
