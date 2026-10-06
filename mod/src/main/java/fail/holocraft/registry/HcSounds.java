package fail.holocraft.registry;

import fail.holocraft.HoloCraft;
import fail.holocraft.sheet.HoloRefs;
import fail.holocraft.sheet.SoundsRow;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Sound events. Every HoloCure sound any sheet names gets holocraft:sfx.<name>; sounds.json rows with a holocraft: event
 * get that event too. Which file plays is decided on the client (HoloCureImport / HoloSfx).
 */
public final class HcSounds {
    private static final Map<String, SoundEvent> SFX = new HashMap<>();
    private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

    public static Identifier sfxId(String holocureName) {
        return HoloCraft.id("sfx." + holocureName.toLowerCase(Locale.ROOT));
    }

    public static void init() {
        for (String s : HoloRefs.SOUNDS) {
            Identifier id = sfxId(s);
            SFX.put(s, Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
        }
        for (SoundsRow r : SoundsRow.ALL) {
            Identifier id = Identifier.of(r.event());
            if (id.getNamespace().equals(HoloCraft.MOD_ID))
                EVENTS.put(id.getPath(), Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
        }
    }

    /** holocraft:sfx.<name> for a HoloCure sound named in a sheet. */
    public static SoundEvent sfx(String holocureName) { return SFX.get(holocureName); }

    /** A holocraft:<path> event from sounds.json, e.g. "music.stage_night". */
    public static SoundEvent event(String path) { return EVENTS.get(path); }

    private HcSounds() {}
}
