package fail.holocraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

/** HoloCure sprites face right and flip to face left; standing still they keep the way they last faced. */
public final class Facing {
    private static final Map<Entity, Boolean> LAST = new WeakHashMap<>();

    /** True when `e`, moving along (dx, dz), should be drawn facing left on screen. */
    public static boolean flip(Entity e, double dx, double dz) {
        if (dx * dx + dz * dz < 1e-6) return LAST.getOrDefault(e, false);
        Vec3d right = Vec3d.fromPolar(0, MinecraftClient.getInstance().gameRenderer.getCamera().getYaw() + 90);
        boolean f = dx * right.x + dz * right.z < 0;
        LAST.put(e, f);
        return f;
    }

    private Facing() {}
}
