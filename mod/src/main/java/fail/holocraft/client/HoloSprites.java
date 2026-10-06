package fail.holocraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fail.holocraft.HoloCraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** The imported HoloCure sprites: size, origin and frame textures (from hc_index.json). */
public final class HoloSprites {
    public record Info(int frames, int w, int h, int ox, int oy) {}

    private static final Map<String, Info> INDEX = new HashMap<>();

    public static void load() {
        INDEX.clear();
        Path idx = HoloCureImport.indexFile("hc_index.json");
        if (!Files.isRegularFile(idx)) return;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(idx)).getAsJsonObject();
            for (String k : o.keySet()) {
                JsonObject e = o.getAsJsonObject(k);
                INDEX.put(k, new Info(e.get("frames").getAsInt(), e.get("w").getAsInt(), e.get("h").getAsInt(), e.get("ox").getAsInt(), e.get("oy").getAsInt()));
            }
        } catch (Exception e) {
            HoloCraft.LOG.error("hc_index.json unreadable", e);
        }
    }

    public static boolean available() { return !INDEX.isEmpty(); }

    public static Info info(String sprite) { return INDEX.get(sprite); }

    public static Identifier texture(String sprite, int frame) {
        Info i = INDEX.get(sprite);
        int f = i == null ? 0 : Math.floorMod(frame, i.frames());
        return HoloCraft.id("textures/hc/" + HoloCureImport.texPath(sprite) + "/" + f + ".png");
    }

    /** Frame for an animation running at `fps` at time `ticks`. */
    public static int frameAt(String sprite, float ticks, float fps) {
        Info i = INDEX.get(sprite);
        return i == null ? 0 : (int) (ticks / 20f * fps) % i.frames();
    }

    /** Draw a sprite frame in the GUI at (x, y) scaled to w x h. */
    public static void draw(DrawContext ctx, String sprite, int frame, int x, int y, int w, int h) {
        draw(ctx, sprite, frame, x, y, w, h, 0xFFFFFFFF);
    }

    public static void draw(DrawContext ctx, String sprite, int frame, int x, int y, int w, int h, int argb) {
        Info i = INDEX.get(sprite);
        if (i == null) return;
        ctx.drawTexture(RenderLayer::getGuiTextured, texture(sprite, frame), x, y, 0, 0, w, h, i.w(), i.h(), i.w(), i.h(), argb);
    }

    /** Draw at native size times `scale`, top-left at (x, y). */
    public static void drawScaled(DrawContext ctx, String sprite, int frame, int x, int y, float scale) {
        Info i = INDEX.get(sprite);
        if (i == null) return;
        draw(ctx, sprite, frame, x, y, Math.round(i.w() * scale), Math.round(i.h() * scale));
    }

    /** Draw a sub-rectangle (u, v, rw, rh) of a sprite frame scaled into x, y, w, h. */
    public static void drawRegion(DrawContext ctx, String sprite, int frame, int x, int y, int w, int h, int u, int v, int rw, int rh, int argb) {
        Info i = INDEX.get(sprite);
        if (i == null) return;
        ctx.drawTexture(RenderLayer::getGuiTextured, texture(sprite, frame), x, y, u, v, w, h, rw, rh, i.w(), i.h(), argb);
    }

    private HoloSprites() {}
}
