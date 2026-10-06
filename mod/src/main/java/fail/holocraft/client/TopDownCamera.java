package fail.holocraft.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * systems: topdown_camera. V flips view_mode between Minecraft's camera (the stage in 3D) and HoloCure's own view: the
 * camera straight above the player looking down, orthographic at HoloCure's pixel scale (ortho_projection), north up.
 * The player always faces north there, so W/A/S/D move up/left/down/right on screen; idols aim themselves.
 */
public final class TopDownCamera {
    /** How high above the player's feet the 2D camera hangs; orthographic, so it only has to clear the scenery. */
    public static final float HEIGHT = 40f, PLAYER_PITCH = 58f;
    private static KeyBinding key;

    public static void init() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.holocraft.camera", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.holocraft"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) {
                ViewMode.mode = ViewMode.flat() ? ViewMode.Mode.WORLD_3D : ViewMode.Mode.ORIGINAL_2D;
                // looking straight down, vanilla's cave culling walks the sections from the camera's and drops
                // everything past the first chunk border; the flat view draws every section in range instead
                mc.chunkCullingEnabled = !ViewMode.flat();
                mc.worldRenderer.scheduleTerrainUpdate();
                if (mc.player != null)
                    mc.player.sendMessage(Text.literal(ViewMode.flat() ? "HoloCure view (V for Minecraft view)" : "Minecraft view (V for HoloCure view)"), true);
            }
            if (ViewMode.flat() && mc.player != null) {
                mc.player.setYaw(180f);
                mc.player.setHeadYaw(180f);
                mc.player.setPitch(PLAYER_PITCH);
            }
        });
    }

    private TopDownCamera() {}
}
