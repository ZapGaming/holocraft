package fail.holocraft.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fail.holocraft.HoloCraft;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * systems: stage_layout. Where each prop stands on a stage, in blocks, read from the player's own HoloCure by the
 * importer (holocraft/pack/stages/<stage>.json). The map repeats every widthBlocks x heightBlocks.
 */
public final class StageLayouts {
    public record Prop(String id, int x, int z) {}
    public record Layout(int widthBlocks, int heightBlocks, List<Prop> props) {
        public static final Layout EMPTY = new Layout(1, 1, List.of());
    }

    private static final Map<String, Layout> CACHE = new ConcurrentHashMap<>();

    public static Path file(String stage) {
        return FabricLoader.getInstance().getGameDir().resolve("holocraft/pack/stages/" + stage + ".json");
    }

    public static Layout get(String stage) {
        return CACHE.computeIfAbsent(stage, StageLayouts::load);
    }

    private static Layout load(String stage) {
        Path f = file(stage);
        if (!Files.isRegularFile(f)) {
            HoloCraft.LOG.warn("No layout for stage {} at {}: the stage floor generates without props", stage, f);
            return Layout.EMPTY;
        }
        try {
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            int px = o.get("px_per_block").getAsInt();
            List<Prop> props = new ArrayList<>();
            for (var e : o.getAsJsonArray("props")) {
                JsonArray a = e.getAsJsonArray();
                props.add(new Prop(a.get(0).getAsString(), Math.floorDiv(a.get(1).getAsInt(), px), Math.floorDiv(a.get(2).getAsInt(), px)));
            }
            Layout l = new Layout(Math.max(1, o.get("room_w").getAsInt() / px), Math.max(1, o.get("room_h").getAsInt() / px), props);
            HoloCraft.LOG.info("Stage {}: {} props on a {}x{} block map", stage, props.size(), l.widthBlocks(), l.heightBlocks());
            return l;
        } catch (Exception ex) {
            HoloCraft.LOG.error("Bad stage layout " + f, ex);
            return Layout.EMPTY;
        }
    }

    private StageLayouts() {}
}
