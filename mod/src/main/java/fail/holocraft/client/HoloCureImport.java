package fail.holocraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fail.holocraft.HoloCraft;
import fail.holocraft.holocure.GmData;
import fail.holocraft.holocure.GmImage;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Builds the HoloCraft resource pack on the player's PC from their own HoloCure: every sprite the sheets name,
 * the GUI overrides (ui.json), fonts (fonts.json), sounds (sounds.json + every sound a sheet names), the logo and
 * the item icons. Nothing of HoloCure's ships with the mod; this is where it comes from.
 */
public final class HoloCureImport {
    /** Bump when the importer's output changes so existing packs are rebuilt. */
    public static final int IMPORTER_VERSION = 4;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path packDir() { return FabricLoader.getInstance().getGameDir().resolve("holocraft/pack"); }

    /** Assets that are read directly by the client (sprite index, sfx index) rather than through resources. */
    public static Path indexFile(String name) { return packDir().resolve(name); }

    public static String texPath(String sprite) { return sprite.toLowerCase(Locale.ROOT); }

    public static String soundPath(String sound) { return sound.toLowerCase(Locale.ROOT); }

    /** Returns true when a usable pack exists (fresh or already built). */
    public static boolean ensure() {
        Path holo = HoloCureLocator.find();
        Path pack = packDir();
        Path stampFile = pack.resolve("holocraft-import.json");
        if (holo == null) return Files.isRegularFile(stampFile);
        try {
            String stamp = stamp(holo);
            if (Files.isRegularFile(stampFile) && Files.readString(stampFile).equals(stamp)) return true;
            long t0 = System.currentTimeMillis();
            run(holo, pack);
            Files.writeString(stampFile, stamp);
            HoloCraft.LOG.info("HoloCraft pack built from {} in {} ms", holo, System.currentTimeMillis() - t0);
            return true;
        } catch (Exception e) {
            HoloCraft.LOG.error("Could not build the HoloCraft pack from " + holo, e);
            return Files.isRegularFile(stampFile);
        }
    }

    static String stamp(Path holo) throws IOException {
        String ver = Files.isRegularFile(holo.resolve("version.ini")) ? Files.readString(holo.resolve("version.ini")).trim() : "?";
        return "{\"importer\":" + IMPORTER_VERSION + ",\"holocure\":\"" + ver.replaceAll("[^0-9A-Za-z.=\\[\\] ]", "")
                + "\",\"size\":" + Files.size(holo.resolve("data.win")) + ",\"refs\":" + fail.holocraft.sheet.HoloRefs.SPRITES.hashCode() + "}";
    }

    static void run(Path holo, Path pack) throws IOException {
        deleteTree(pack);
        Files.createDirectories(pack);
        GmData gm = new GmData(holo.resolve("data.win"));
        Path assets = pack.resolve("assets");
        write(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":46,\"description\":\"HoloCraft: built from your HoloCure\"}}");

        // 1. sprites named by any sheet
        JsonObject index = new JsonObject();
        for (String s : HoloRefs.SPRITES) {
            GmData.Sprite sp = gm.sprites().get(s);
            if (sp == null || sp.frames().length == 0) { HoloCraft.LOG.warn("HoloCure has no sprite {}", s); continue; }
            int n = Math.min(sp.frames().length, 48);
            GmImage first = null;
            for (int f = 0; f < n; f++) {
                GmImage img = gm.frame(s, f);
                if (first == null) first = img;
                img.writePng(assets.resolve("holocraft/textures/hc/" + texPath(s) + "/" + f + ".png"));
            }
            JsonObject o = new JsonObject();
            o.addProperty("frames", n);
            o.addProperty("w", first.width());
            o.addProperty("h", first.height());
            o.addProperty("ox", sp.originX());
            o.addProperty("oy", sp.originY());
            index.add(s, o);
        }

        // 2. item icons
        for (IdolsRow r : IdolsRow.ALL) square(gm.frame(r.portraitSprite(), 0)).writePng(assets.resolve("holocraft/textures/item/idol_" + r.id() + ".png"));
        for (ItemsRow r : ItemsRow.ALL) square(gm.frame(r.sprite(), 0)).writePng(assets.resolve("holocraft/textures/item/" + r.id() + ".png"));

        // 3. GUI overrides
        for (UiRow r : UiRow.ALL) {
            GmImage src = gm.frame(r.source(), r.frame());
            GmImage out = switch (r.mode()) {
                case "nine_slice" -> src.nineSlice(r.outW(), r.outH(), r.border());
                case "tile_row" -> {
                    GmImage o = GmImage.blank(r.outW(), r.outH());
                    GmImage cell = src.scale(r.outH() - 2, r.outH() - 2);
                    for (int i = 0; i < 9; i++) o.draw(cell, 1 + i * (r.outH() - 2), 1);
                    yield o;
                }
                case "fit" -> src.cover(r.outW(), r.outH());
                case "solid" -> {
                    int[] px = new int[r.outW() * r.outH()];
                    Arrays.fill(px, 0xCC000000 | Integer.parseInt(r.tint(), 16));
                    yield new GmImage(r.outW(), r.outH(), px);
                }
                case "orb_atlas" -> {
                    GmImage o = GmImage.blank(r.outW(), r.outH());
                    int cell = r.outW() / 4, frames = gm.frameCount(r.source());
                    for (int i = 0; i < 16; i++) o.draw(gm.frame(r.source(), i % frames).scale(cell, cell), (i % 4) * cell, (i / 4) * cell);
                    yield o;
                }
                default -> src.scale(r.outW(), r.outH());
            };
            if (!r.tint().equals("none") && !r.mode().equals("solid")) out = out.tint(Integer.parseInt(r.tint(), 16));
            Path dst = assets.resolve("minecraft/textures/" + r.mcPath());
            out.writePng(dst);
            if (r.mode().equals("nine_slice")) {
                // our own slice: HoloCure's corners are wider than Minecraft's 3 px, and the middle must stretch, not tile
                write(Path.of(dst + ".mcmeta"), "{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":" + r.outW() + ",\"height\":" + r.outH()
                        + ",\"border\":" + Math.min(r.border(), r.outH() / 2) + ",\"stretch_inner\":true}}}");
            } else copyVanilla("assets/minecraft/textures/" + r.mcPath() + ".mcmeta", Path.of(dst + ".mcmeta"));
        }

        // 4. fonts
        for (FontsRow r : FontsRow.ALL) writeFont(gm, r, assets);

        // 5. logo
        GmImage logo = buildLogo(gm);
        logo.writePng(assets.resolve("holocraft/textures/gui/logo.png"));
        JsonObject lo = new JsonObject();
        lo.addProperty("frames", 1); lo.addProperty("w", logo.width()); lo.addProperty("h", logo.height());
        lo.addProperty("ox", 0); lo.addProperty("oy", 0);
        index.add("_logo", lo);
        write(pack.resolve("hc_index.json"), GSON.toJson(index));

        // 6. sounds
        writeSounds(holo, gm, assets, pack);

        // 7. stages: floor blocks cut from the stage's floor texture, prop models, and where every prop stands
        for (StagesRow st : StagesRow.ALL) writeStage(gm, st, assets, pack);
    }

    static void writeStage(GmData gm, StagesRow st, Path assets, Path pack) throws IOException {
        String ground = st.id() + "_ground";
        int n = Math.min(fail.holocraft.world.StageGroundBlock.MAX_CELLS, st.groundCells());
        GmImage floor = gm.frame(st.groundSprite(), 0);
        int cw = floor.width() / n, ch = floor.height() / n, size = 32;
        for (int i = 0; i < n * n; i++) {
            GmImage cell = floor.crop((i % n) * cw, (i / n) * ch, cw, ch).scale(size, size);
            cell.writePng(assets.resolve("holocraft/textures/block/" + ground + "/" + i + ".png"));
            JsonObject tex = new JsonObject();
            tex.addProperty("top", "holocraft:block/" + ground + "/" + i);
            tex.addProperty("side", "holocraft:block/" + ground + "/side");
            tex.addProperty("bottom", "minecraft:block/dirt");
            tex.addProperty("particle", "holocraft:block/" + ground + "/" + i);
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:block/cube_bottom_top");
            model.add("textures", tex);
            write(assets.resolve("holocraft/models/block/" + ground + "/" + i + ".json"), GSON.toJson(model));
        }
        // side: the floor's top strip over darkened earth, so a dug edge still reads as this stage
        GmImage side = floor.crop(0, 0, cw, ch).scale(size, size).tint(0x8A7A6A);
        side.draw(floor.crop(0, 0, cw, Math.max(1, ch / 5)).scale(size, size / 5), 0, 0);
        side.writePng(assets.resolve("holocraft/textures/block/" + ground + "/side.png"));
        JsonObject variants = new JsonObject();
        int max = fail.holocraft.world.StageGroundBlock.MAX_CELLS * fail.holocraft.world.StageGroundBlock.MAX_CELLS;
        for (int i = 0; i < max; i++) {
            JsonObject v = new JsonObject();
            v.addProperty("model", "holocraft:block/" + ground + "/" + (i % (n * n)));
            variants.add("cell=" + i, v);
        }
        JsonObject bs = new JsonObject();
        bs.add("variants", variants);
        write(assets.resolve("holocraft/blockstates/" + ground + ".json"), GSON.toJson(bs));
        write(assets.resolve("holocraft/items/" + ground + ".json"),
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"holocraft:block/" + ground + "/0\"}}");

        JsonArray props = new JsonArray();
        for (PropsRow p : PropsRow.ALL) {
            if (!p.stage().equals(st.id())) continue;
            String id = "prop_" + p.id(), tex = "holocraft:hc/" + texPath(p.sprite()) + "/0";
            write(assets.resolve("holocraft/blockstates/" + id + ".json"), "{\"variants\":{\"\":{\"model\":\"holocraft:block/" + id + "\"}}}");
            write(assets.resolve("holocraft/models/block/" + id + ".json"), "{\"textures\":{\"particle\":\"" + tex + "\"}}");
            write(assets.resolve("holocraft/models/item/" + id + ".json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + tex + "\"}}");
            write(assets.resolve("holocraft/items/" + id + ".json"), "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"holocraft:item/" + id + "\"}}");
            for (GmData.Placement pl : gm.roomInstances(st.room())) {
                if (!pl.object().equals(p.object())) continue;
                JsonArray a = new JsonArray();
                a.add(p.id()); a.add(pl.x()); a.add(pl.y());
                props.add(a);
            }
        }
        int[] room = gm.roomSize(st.room());
        JsonObject layout = new JsonObject();
        layout.addProperty("room_w", room == null ? 3840 : room[0]);
        layout.addProperty("room_h", room == null ? 3840 : room[1]);
        layout.addProperty("px_per_block", st.mapPxPerBlock());
        layout.add("props", props);
        write(pack.resolve("stages/" + st.id() + ".json"), GSON.toJson(layout));
    }

    static GmImage square(GmImage img) {
        int s = Math.max(img.width(), img.height());
        GmImage o = GmImage.blank(s, s);
        o.draw(img, (s - img.width()) / 2, (s - img.height()) / 2);
        return o;
    }

    // ---------- fonts ----------

    record Glyph(int ch, GmImage img, int shift, int offset) {}

    static Map<Integer, Glyph> glyphs(GmData gm, String font) throws IOException {
        GmData.Font f = gm.fonts().get(font);
        if (f == null) throw new IOException("HoloCure has no font " + font);
        GmData.Tpag t = gm.tpag(f.tpag());
        GmImage page = gm.region(t, t.bw(), t.bh()).crop(t.tx(), t.ty(), t.tw(), t.th());
        Map<Integer, Glyph> m = new TreeMap<>();
        for (GmData.Glyph g : f.glyphs()) m.put(g.ch(), new Glyph(g.ch(), page.crop(g.x(), g.y(), g.w(), g.h()), g.shift(), g.offset()));
        return m;
    }

    static void writeFont(GmData gm, FontsRow r, Path assets) throws IOException {
        Map<Integer, Glyph> gl = glyphs(gm, r.source());
        List<Glyph> drawn = gl.values().stream().filter(g -> g.ch() > 32 && hasInk(g.img())).toList();
        int cw = 1, ch = 1;
        for (Glyph g : drawn) { cw = Math.max(cw, g.img().width() + Math.max(0, g.offset())); ch = Math.max(ch, g.img().height()); }
        int cols = 16, rows = (drawn.size() + cols - 1) / cols;
        GmImage sheet = GmImage.blank(cw * cols, ch * rows);
        JsonArray chars = new JsonArray();
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < rows * cols; i++) {
            if (i < drawn.size()) {
                Glyph g = drawn.get(i);
                sheet.draw(g.img(), (i % cols) * cw + Math.max(0, g.offset()), (i / cols) * ch);
                row.appendCodePoint(g.ch());
            } else row.append('\u0000');
            if (i % cols == cols - 1) { chars.add(row.toString()); row.setLength(0); }
        }
        String[] id = r.mcFont().split(":");
        String tex = "holocraft:font/" + r.source().toLowerCase(Locale.ROOT) + ".png";
        sheet.writePng(assets.resolve("holocraft/textures/font/" + r.source().toLowerCase(Locale.ROOT) + ".png"));
        JsonObject bitmap = new JsonObject();
        bitmap.addProperty("type", "bitmap");
        bitmap.addProperty("file", tex);
        bitmap.addProperty("height", r.height());
        bitmap.addProperty("ascent", r.ascent());
        bitmap.add("chars", chars);
        JsonArray providers = new JsonArray();
        providers.add(ref("minecraft:include/space"));
        providers.add(bitmap);
        if (r.fallback()) { providers.add(ref("minecraft:include/default")); providers.add(ref("minecraft:include/unifont")); }
        JsonObject root = new JsonObject();
        root.add("providers", providers);
        write(assets.resolve(id[0] + "/font/" + id[1] + ".json"), GSON.toJson(root));
    }

    static JsonObject ref(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "reference");
        o.addProperty("id", id);
        return o;
    }

    static boolean hasInk(GmImage img) {
        for (int c : img.argb()) if ((c >>> 24) > 0) return true;
        return false;
    }

    // ---------- logo ----------

    static GmImage text(Map<Integer, Glyph> gl, String s, int rgb) {
        int w = 8, h = 1;
        for (char c : s.toCharArray()) { Glyph g = gl.get((int) c); if (g != null) { w += g.shift(); h = Math.max(h, g.img().height()); } }
        GmImage o = GmImage.blank(w, h);
        int x = 0;
        for (char c : s.toCharArray()) {
            Glyph g = gl.get((int) c);
            if (g == null) continue;
            o.draw(solid(g.img(), rgb), x + g.offset(), 0);
            x += g.shift();
        }
        return o;
    }

    static GmImage solid(GmImage img, int rgb) {
        int[] px = new int[img.argb().length];
        for (int i = 0; i < px.length; i++) px[i] = (img.argb()[i] & 0xFF000000) | (rgb & 0xFFFFFF);
        return new GmImage(img.width(), img.height(), px);
    }

    static GmImage dilate(GmImage img, int r, int rgb) {
        int w = img.width(), h = img.height();
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int a = 0;
                for (int dy = -r; dy <= r && a < 255; dy++)
                    for (int dx = -r; dx <= r; dx++) {
                        int xx = x + dx, yy = y + dy;
                        if (xx >= 0 && yy >= 0 && xx < w && yy < h) a = Math.max(a, img.get(xx, yy) >>> 24);
                    }
                px[y * w + x] = (a << 24) | (rgb & 0xFFFFFF);
            }
        return new GmImage(w, h, px);
    }

    /** "HoloCraft" in HoloCure's title lettering and colours. */
    static GmImage buildLogo(GmData gm) throws IOException {
        FontsRow logoFont = FontsRow.ALL.stream().filter(f -> f.mcFont().equals("holocraft:logo")).findFirst().orElseThrow();
        Map<Integer, Glyph> gl = glyphs(gm, logoFont.source());
        GmImage holo = text(gl, "Holo", 0x3BA3FF), craft = text(gl, "Craft", 0xFF8AC4);
        int pad = 10;
        GmImage base = GmImage.blank(holo.width() + craft.width() + 2 * pad, Math.max(holo.height(), craft.height()) + 2 * pad);
        base.draw(holo, pad, pad);
        base.draw(craft, pad + holo.width(), pad);
        GmImage logo = GmImage.blank(base.width(), base.height());
        logo.draw(dilate(base, 7, 0x244ABE), 0, 0);
        logo.draw(dilate(base, 4, 0xFFFFFF), 0, 0);
        logo.draw(base, 0, 0);
        return logo;
    }

    // ---------- sounds ----------

    static void writeSounds(Path holo, GmData gm, Path assets, Path pack) throws IOException {
        Map<Integer, GmData> groups = new HashMap<>();
        Map<String, String> files = new HashMap<>(); // holocure sound -> written file name
        Set<String> wanted = new TreeSet<>(HoloRefs.SOUNDS);
        for (String s : wanted) {
            GmData.Sound snd = gm.sounds().get(s);
            if (snd == null) { HoloCraft.LOG.warn("HoloCure has no sound {}", s); continue; }
            GmData src = snd.group() == 0 ? gm : groups.computeIfAbsent(snd.group(), g -> {
                try {
                    Path f = holo.resolve("audiogroup" + g + ".dat");
                    return Files.isRegularFile(f) ? new GmData(f) : null;
                } catch (IOException e) { return null; }
            });
            byte[] data = src == null || !src.has("AUDO") ? null : src.audio(snd.audioId());
            if (data == null) { HoloCraft.LOG.warn("No audio data for {}", s); continue; }
            boolean ogg = data.length > 4 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g';
            String name = soundPath(s) + (ogg ? ".ogg" : ".wav");
            Path dst = assets.resolve("holocraft/sounds/" + name);
            Files.createDirectories(dst.getParent());
            Files.write(dst, data);
            files.put(s, name);
        }

        // sounds.json for OGG (Minecraft plays those itself); sfx_index.json for WAV (HoloSfx plays those).
        JsonObject holoSounds = new JsonObject(), mcSounds = new JsonObject(), sfx = new JsonObject();
        for (String s : files.keySet()) {
            String file = files.get(s);
            String event = HcSounds.sfxId(s).getPath();
            if (file.endsWith(".ogg")) holoSounds.add(event, soundEntry(file, s.startsWith("bgm_"), 1f, false));
            else sfx.add("holocraft:" + event, sfxEntry(file, 1f));
        }
        for (SoundsRow r : SoundsRow.ALL) {
            String file = files.get(r.source());
            if (file == null) continue;
            String[] ev = r.event().split(":");
            if (file.endsWith(".ogg")) {
                JsonObject e = soundEntry(file, r.stream(), r.volume(), ev[0].equals("minecraft"));
                (ev[0].equals("minecraft") ? mcSounds : holoSounds).add(ev[1], e);
            } else sfx.add(r.event(), sfxEntry(file, r.volume()));
        }
        write(assets.resolve("holocraft/sounds.json"), GSON.toJson(holoSounds));
        if (mcSounds.size() > 0) write(assets.resolve("minecraft/sounds.json"), GSON.toJson(mcSounds));
        write(pack.resolve("sfx_index.json"), GSON.toJson(sfx));
    }

    static JsonObject soundEntry(String file, boolean stream, float volume, boolean replace) {
        JsonObject s = new JsonObject();
        s.addProperty("name", "holocraft:" + file.substring(0, file.lastIndexOf('.')));
        s.addProperty("stream", stream);
        s.addProperty("volume", volume);
        JsonArray arr = new JsonArray();
        arr.add(s);
        JsonObject e = new JsonObject();
        if (replace) e.addProperty("replace", true);
        e.add("sounds", arr);
        return e;
    }

    static JsonObject sfxEntry(String file, float volume) {
        JsonObject o = new JsonObject();
        o.addProperty("file", "assets/holocraft/sounds/" + file);
        o.addProperty("volume", volume);
        return o;
    }

    // ---------- io ----------

    static void copyVanilla(String resource, Path dst) throws IOException {
        try (InputStream in = HoloCureImport.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) Files.write(dst, in.readAllBytes());
        }
    }

    static void write(Path p, String s) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, s, StandardCharsets.UTF_8);
    }

    static void deleteTree(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var walk = Files.walk(p)) {
            for (Path q : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(q);
        }
    }

    private HoloCureImport() {}
}
