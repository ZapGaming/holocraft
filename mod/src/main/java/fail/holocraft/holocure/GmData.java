package fail.holocraft.holocure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Read-only view of a GameMaker 2022+ data.win (or an audiogroupN.dat), enough for sprites, fonts, sounds and strings. */
public final class GmData {
    public record Sprite(String name, int width, int height, int originX, int originY, int[] frames) {}
    public record Tpag(int sx, int sy, int sw, int sh, int tx, int ty, int tw, int th, int bw, int bh, int tex) {}
    public record Glyph(int ch, int x, int y, int w, int h, int shift, int offset) {}
    public record Font(String name, int tpag, List<Glyph> glyphs) {}
    public record Sound(String name, String file, int group, int audioId) {}

    private final ByteBuffer d;
    private final Map<String, int[]> chunks = new HashMap<>();
    private Map<String, Sprite> sprites;
    private Map<String, Font> fonts;
    private Map<String, Sound> sounds;
    private final Map<Integer, int[]> pages = new HashMap<>();
    private final Map<Integer, int[]> pageSize = new HashMap<>();
    private int[] texOffsets;
    private int[] texEnds;

    public GmData(Path file) throws IOException {
        d = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        if (d.getInt(0) != 0x4D524F46) throw new IOException(file + " is not a GameMaker FORM file");
        int p = 8;
        while (p + 8 <= d.capacity()) {
            String n = new String(new byte[]{d.get(p), d.get(p + 1), d.get(p + 2), d.get(p + 3)}, StandardCharsets.US_ASCII);
            int sz = d.getInt(p + 4);
            chunks.put(n, new int[]{p + 8, sz});
            p += 8 + sz;
        }
    }

    public boolean has(String chunk) { return chunks.containsKey(chunk); }
    private int u32(int o) { return d.getInt(o); }
    private int u16(int o) { return d.getShort(o) & 0xFFFF; }
    private int s16(int o) { return d.getShort(o); }

    public String strAt(int o) {
        if (o == 0) return null;
        int n = u32(o - 4);
        byte[] b = new byte[n];
        d.get(o, b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private int[] plist(int o) {
        int n = u32(o);
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = u32(o + 4 + 4 * i);
        return r;
    }

    public Map<String, Sprite> sprites() {
        if (sprites != null) return sprites;
        sprites = new HashMap<>();
        for (int p : plist(chunks.get("SPRT")[0])) {
            String name = strAt(u32(p));
            int w = u32(p + 4), h = u32(p + 8);
            int ox = u32(p + 0x30), oy = u32(p + 0x34);
            int q = p + 0x38, type = 0;
            if (u32(q) == -1) {
                int ver = u32(q + 4);
                type = u32(q + 8);
                q += 0x14;
                if (ver >= 2) q += 4;
                if (ver >= 3) q += 4;
            }
            sprites.put(name, new Sprite(name, w, h, ox, oy, type == 0 ? plist(q) : new int[0]));
        }
        return sprites;
    }

    public Tpag tpag(int p) {
        return new Tpag(u16(p), u16(p + 2), u16(p + 4), u16(p + 6), u16(p + 8), u16(p + 10), u16(p + 12), u16(p + 14), u16(p + 16), u16(p + 18), u16(p + 20));
    }

    public Map<String, Font> fonts() {
        if (fonts != null) return fonts;
        fonts = new HashMap<>();
        int[] fc = chunks.get("FONT");
        for (int p : plist(fc[0])) {
            String name = strAt(u32(p));
            int tp = u32(p + 0x1C);
            for (int k = 0; k < 8; k++) {
                int q = p + 0x28 + 4 * k, n = u32(q);
                if (n <= 0 || n >= 5000) continue;
                int f0 = u32(q + 4), fl = u32(q + 4 * n);
                if (f0 <= fc[0] || f0 >= fc[0] + fc[1] || fl <= fc[0] || fl >= fc[0] + fc[1]) continue;
                List<Glyph> gl = new ArrayList<>();
                for (int g : plist(q)) gl.add(new Glyph(u16(g), u16(g + 2), u16(g + 4), u16(g + 6), u16(g + 8), s16(g + 10), s16(g + 12)));
                fonts.put(name, new Font(name, tp, gl));
                break;
            }
        }
        return fonts;
    }

    public Map<String, Sound> sounds() {
        if (sounds != null) return sounds;
        sounds = new HashMap<>();
        for (int p : plist(chunks.get("SOND")[0])) {
            String name = strAt(u32(p));
            sounds.put(name, new Sound(name, strAt(u32(p + 12)), u32(p + 28), u32(p + 32)));
        }
        return sounds;
    }

    public record Placement(String object, int x, int y) {}

    /** Every object instance placed in a room (the room's instance list), by object name. */
    public List<Placement> roomInstances(String room) {
        List<String> objects = new ArrayList<>();
        for (int p : plist(chunks.get("OBJT")[0])) objects.add(strAt(u32(p)));
        for (int p : plist(chunks.get("ROOM")[0])) {
            if (!room.equals(strAt(u32(p)))) continue;
            List<Placement> out = new ArrayList<>();
            for (int ip : plist(u32(p + 48))) {
                int x = u32(ip), y = u32(ip + 4), obj = u32(ip + 8);
                if (obj >= 0 && obj < objects.size()) out.add(new Placement(objects.get(obj), x, y));
            }
            return out;
        }
        return List.of();
    }

    /** {width, height} of a room in pixels. */
    public int[] roomSize(String room) {
        for (int p : plist(chunks.get("ROOM")[0]))
            if (room.equals(strAt(u32(p)))) return new int[]{u32(p + 8), u32(p + 12)};
        return null;
    }

    public Set<String> strings() {
        Set<String> s = new HashSet<>();
        for (int p : plist(chunks.get("STRG")[0])) s.add(strAt(p + 4));
        return s;
    }

    /** Raw bytes of embedded audio #id in this file's AUDO chunk (WAV or OGG). */
    public byte[] audio(int id) {
        int[] list = plist(chunks.get("AUDO")[0]);
        if (id < 0 || id >= list.length) return null;
        int p = list[id], n = u32(p);
        byte[] b = new byte[n];
        d.get(p + 4, b);
        return b;
    }

    /** Texture page as ARGB pixels; size via pageSize(). */
    public int[] page(int index) throws IOException {
        int[] px = pages.get(index);
        if (px != null) return px;
        if (texOffsets == null) {
            int[] tc = chunks.get("TXTR");
            int[] list = plist(tc[0]);
            texOffsets = new int[list.length];
            for (int i = 0; i < list.length; i++) texOffsets[i] = u32(list[i] + 24);
            int[] sorted = texOffsets.clone();
            Arrays.sort(sorted);
            texEnds = new int[list.length];
            for (int i = 0; i < list.length; i++) {
                int end = tc[0] + tc[1];
                for (int s : sorted) if (s > texOffsets[i]) { end = s; break; }
                texEnds[i] = end;
            }
        }
        byte[] blob = new byte[texEnds[index] - texOffsets[index]];
        d.get(texOffsets[index], blob);
        GmImage img = GmImage.decode(blob);
        pages.put(index, img.argb());
        pageSize.put(index, new int[]{img.width(), img.height()});
        return img.argb();
    }

    public int[] pageSize(int index) { return pageSize.get(index); }

    /** One sprite frame as an ARGB image of the sprite's full size (trimmed area placed at its target offset). */
    public GmImage frame(String sprite, int frame) throws IOException {
        Sprite s = sprites().get(sprite);
        if (s == null || s.frames().length == 0) throw new IOException("no sprite " + sprite);
        Tpag t = tpag(s.frames()[Math.floorMod(frame, s.frames().length)]);
        return region(t, Math.max(s.width(), t.bw()), Math.max(s.height(), t.bh()));
    }

    public GmImage region(Tpag t, int outW, int outH) throws IOException {
        int[] px = page(t.tex());
        int pw = pageSize(t.tex())[0];
        int[] out = new int[outW * outH];
        for (int y = 0; y < t.th() && t.ty() + y < outH; y++) {
            int sy = t.sy() + (t.sh() == t.th() ? y : y * t.sh() / t.th());
            for (int x = 0; x < t.tw() && t.tx() + x < outW; x++) {
                int sx = t.sx() + (t.sw() == t.tw() ? x : x * t.sw() / t.tw());
                out[(t.ty() + y) * outW + t.tx() + x] = px[sy * pw + sx];
            }
        }
        return new GmImage(outW, outH, out);
    }

    public int frameCount(String sprite) {
        Sprite s = sprites().get(sprite);
        return s == null ? 0 : s.frames().length;
    }
}
