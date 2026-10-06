package fail.holocraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fail.holocraft.HoloCraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plays HoloCure's WAV sound effects (Minecraft's engine only reads OGG). sfx_index.json maps a sound event id
 * to a file; volume follows Minecraft's master and category sliders, and fades with distance like a normal sound.
 */
public final class HoloSfx {
    record Sfx(byte[] data, float volume) {}

    private static final Map<String, Sfx> SFX = new HashMap<>();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HoloCraft-sfx");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicInteger PLAYING = new AtomicInteger();

    public static void load() {
        SFX.clear();
        Path idx = HoloCureImport.indexFile("sfx_index.json");
        if (!Files.isRegularFile(idx)) return;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(idx)).getAsJsonObject();
            for (String id : o.keySet()) {
                JsonObject e = o.getAsJsonObject(id);
                Path f = HoloCureImport.packDir().resolve(e.get("file").getAsString());
                if (Files.isRegularFile(f)) SFX.put(id, new Sfx(Files.readAllBytes(f), e.get("volume").getAsFloat()));
            }
            HoloCraft.LOG.info("HoloSfx: {} HoloCure sound effects", SFX.size());
        } catch (Exception e) {
            HoloCraft.LOG.error("HoloSfx index unreadable", e);
        }
    }

    /** True when the sound was ours (and is now playing), so Minecraft should not try it. */
    public static boolean play(SoundInstance sound) {
        Sfx s = SFX.get(sound.getId().toString());
        if (s == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        float gain = s.volume * mc.options.getSoundVolume(SoundCategory.MASTER) * mc.options.getSoundVolume(sound.getCategory());
        if (sound.getAttenuationType() != SoundInstance.AttenuationType.NONE && mc.player != null && !sound.isRelative()) {
            double d = Math.sqrt(mc.player.squaredDistanceTo(sound.getX(), sound.getY(), sound.getZ()));
            gain *= (float) Math.max(0, 1 - d / 16.0);
        }
        play(s.data, gain);
        return true;
    }

    public static void play(byte[] data, float gain) {
        if (gain <= 0.01f || PLAYING.get() > 24) return;
        EXEC.execute(() -> {
            try {
                AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), (float) (20 * Math.log10(Math.min(1f, gain))))));
                }
                PLAYING.incrementAndGet();
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) { clip.close(); PLAYING.decrementAndGet(); }
                });
                clip.start();
            } catch (Exception e) {
                HoloCraft.LOG.debug("sfx failed: {}", e.toString());
            }
        });
    }

    private HoloSfx() {}
}
