package fail.holocraft.net;

import fail.holocraft.HoloCraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/** Every message between the HoloCraft server and client. */
public final class Payloads {

    /** An attack/special visual: draw `sprite` at (x,y,z) facing `yaw`, stretched to `length` blocks, for `life` ticks. */
    public record Fx(String sprite, double x, double y, double z, float yaw, float length, float scale, int life) implements CustomPayload {
        public static final Id<Fx> ID = new Id<>(HoloCraft.id("fx"));
        public static final PacketCodec<RegistryByteBuf, Fx> CODEC = PacketCodec.ofStatic((buf, v) -> {
            buf.writeString(v.sprite); buf.writeDouble(v.x); buf.writeDouble(v.y); buf.writeDouble(v.z);
            buf.writeFloat(v.yaw); buf.writeFloat(v.length); buf.writeFloat(v.scale); buf.writeVarInt(v.life);
        }, buf -> new Fx(buf.readString(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Show a dialog.json line. */
    public record Dialog(String id) implements CustomPayload {
        public static final Id<Dialog> ID = new Id<>(HoloCraft.id("dialog"));
        public static final PacketCodec<RegistryByteBuf, Dialog> CODEC = PacketCodec.ofStatic((buf, v) -> buf.writeString(v.id), buf -> new Dialog(buf.readString()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Open the idol select. */
    public record ChooseStarter(List<String> idols) implements CustomPayload {
        public static final Id<ChooseStarter> ID = new Id<>(HoloCraft.id("choose_starter"));
        public static final PacketCodec<RegistryByteBuf, ChooseStarter> CODEC = PacketCodec.ofStatic((buf, v) -> {
            buf.writeVarInt(v.idols.size()); v.idols.forEach(buf::writeString);
        }, buf -> {
            int n = buf.readVarInt(); List<String> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(buf.readString());
            return new ChooseStarter(l);
        });
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PickStarter(String idol) implements CustomPayload {
        public static final Id<PickStarter> ID = new Id<>(HoloCraft.id("pick_starter"));
        public static final PacketCodec<RegistryByteBuf, PickStarter> CODEC = PacketCodec.ofStatic((buf, v) -> buf.writeString(v.idol), buf -> new PickStarter(buf.readString()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** kind: 0 = new idol, 1 = idol weapon level up, 2 = new item, 3 = item level up, 4 = hamburger. */
    public record Option(int kind, String id, int level) {}

    public record LevelUp(int level, List<Option> options) implements CustomPayload {
        public static final Id<LevelUp> ID = new Id<>(HoloCraft.id("level_up"));
        public static final PacketCodec<RegistryByteBuf, LevelUp> CODEC = PacketCodec.ofStatic((buf, v) -> {
            buf.writeVarInt(v.level);
            buf.writeVarInt(v.options.size());
            for (Option o : v.options) { buf.writeVarInt(o.kind); buf.writeString(o.id); buf.writeVarInt(o.level); }
        }, buf -> {
            int level = buf.readVarInt(), n = buf.readVarInt(); List<Option> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(new Option(buf.readVarInt(), buf.readString(), buf.readVarInt()));
            return new LevelUp(level, l);
        });
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PickUpgrade(int index) implements CustomPayload {
        public static final Id<PickUpgrade> ID = new Id<>(HoloCraft.id("pick_upgrade"));
        public static final PacketCodec<RegistryByteBuf, PickUpgrade> CODEC = PacketCodec.ofStatic((buf, v) -> buf.writeVarInt(v.index), buf -> new PickUpgrade(buf.readVarInt()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Stage night state for the HUD and music. */
    public record Stage(String stage, boolean active, int cleared, boolean boss) implements CustomPayload {
        public static final Id<Stage> ID = new Id<>(HoloCraft.id("stage"));
        public static final PacketCodec<RegistryByteBuf, Stage> CODEC = PacketCodec.ofStatic((buf, v) -> {
            buf.writeString(v.stage); buf.writeBoolean(v.active); buf.writeVarInt(v.cleared); buf.writeBoolean(v.boss);
        }, buf -> new Stage(buf.readString(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean()));
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(Fx.ID, Fx.CODEC);
        PayloadTypeRegistry.playS2C().register(Dialog.ID, Dialog.CODEC);
        PayloadTypeRegistry.playS2C().register(ChooseStarter.ID, ChooseStarter.CODEC);
        PayloadTypeRegistry.playS2C().register(LevelUp.ID, LevelUp.CODEC);
        PayloadTypeRegistry.playS2C().register(Stage.ID, Stage.CODEC);
        PayloadTypeRegistry.playC2S().register(PickStarter.ID, PickStarter.CODEC);
        PayloadTypeRegistry.playC2S().register(PickUpgrade.ID, PickUpgrade.CODEC);
    }

    private Payloads() {}
}
