package fail.holocraft.client;

import fail.holocraft.sheet.StagesRow;

/**
 * systems: view_mode. The one switch every renderer reads. WORLD_3D is the stage as a Minecraft world seen from
 * Minecraft's camera; ORIGINAL_2D is the stage drawn as HoloCure draws it, straight down. Flipping never moves anything.
 */
public final class ViewMode {
    public enum Mode { WORLD_3D, ORIGINAL_2D }

    public static volatile Mode mode = Mode.WORLD_3D;

    public static boolean flat() { return mode == Mode.ORIGINAL_2D; }

    /** HoloCure pixels per block on the current stage (stages.map_px_per_block): the 2D view's scale. */
    public static float mapPxPerBlock() {
        for (StagesRow s : StagesRow.ALL) if (s.id().equals(ClientStage.stage)) return s.mapPxPerBlock();
        return StagesRow.ALL.get(0).mapPxPerBlock();
    }

    private ViewMode() {}
}
