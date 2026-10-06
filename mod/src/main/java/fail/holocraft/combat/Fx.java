package fail.holocraft.combat;

import fail.holocraft.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Sends attack visuals to everyone who can see them. */
public final class Fx {
    public static void send(ServerWorld world, String sprite, Vec3d pos, float yaw, float length, float scale, int life) {
        Payloads.Fx fx = new Payloads.Fx(sprite, pos.x, pos.y, pos.z, yaw, length, scale, life);
        for (ServerPlayerEntity p : PlayerLookup.around(world, pos, 64)) ServerPlayNetworking.send(p, fx);
    }

    private Fx() {}
}
