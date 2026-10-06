package fail.holocraft.server;

import fail.holocraft.HoloCraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.NetworkUtils;

/**
 * systems: coop_relay. Co-op is Minecraft's own Open to LAN carried over the internet by the bundled e4mc relay.
 * HOLOCRAFT_HOST=1 (Melty sets it on every Play) opens the world to friends as soon as it loads; e4mc then writes
 * "Domain assigned: <address>" to latest.log, which is where Melty reads the address for the join link, and a
 * friend's launcher joins it with --server <address>.
 * -Dholocraft.lanOffline=true opens it without account checks, only so two signed-out test clients can join.
 */
public final class CoopInfo {
    private static boolean opened;

    public static void initClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!"1".equals(System.getenv("HOLOCRAFT_HOST")) || opened) return;
            IntegratedServer server = client.getServer();
            if (server == null || server.isRemote()) return;
            opened = true;
            int port = NetworkUtils.findLocalPort();
            server.execute(() -> {
                if (Boolean.getBoolean("holocraft.lanOffline")) server.setOnlineMode(false);
                boolean ok = server.openToLan(null, false, port);
                HoloCraft.LOG.info("HoloCraft opened the world to friends on LAN port {}: {}", port, ok);
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> opened = false);
    }

    private CoopInfo() {}
}
