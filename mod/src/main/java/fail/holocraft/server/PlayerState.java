package fail.holocraft.server;

import fail.holocraft.item.HoloItem;
import fail.holocraft.item.IdolItem;
import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.IdolsRow;
import fail.holocraft.sheet.ItemsRow;
import fail.holocraft.combat.Buffs;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Per-player HoloCraft progress (starter chosen, levels already rewarded) and the level-up pick flow. */
public final class PlayerState extends PersistentState {
    public static final class Entry {
        boolean starter;
        int rewardedLevel;
        int pending;
    }

    private final Map<UUID, Entry> players = new HashMap<>();
    private static final Map<UUID, List<Payloads.Option>> OFFERS = new HashMap<>();

    private static final Type<PlayerState> TYPE = new Type<>(PlayerState::new, PlayerState::read, DataFixTypes.LEVEL);

    static PlayerState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "holocraft_players");
    }

    Entry of(UUID id) { return players.computeIfAbsent(id, k -> new Entry()); }

    private static PlayerState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PlayerState s = new PlayerState();
        for (String k : nbt.getKeys()) {
            NbtCompound c = nbt.getCompound(k);
            Entry e = new Entry();
            e.starter = c.getBoolean("starter");
            e.rewardedLevel = c.getInt("rewarded");
            e.pending = c.getInt("pending");
            s.players.put(UUID.fromString(k), e);
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        players.forEach((id, e) -> {
            NbtCompound c = new NbtCompound();
            c.putBoolean("starter", e.starter);
            c.putInt("rewarded", e.rewardedLevel);
            c.putInt("pending", e.pending);
            nbt.put(id.toString(), c);
        });
        return nbt;
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            PlayerState s = get(server);
            Entry e = s.of(p.getUuid());
            if (!e.starter && allStacks(p).stream().anyMatch(st -> st.getItem() instanceof IdolItem)) {
                // already has an idol (state lost to a crash, or given by hand): never offer a second starter
                e.starter = true;
                e.rewardedLevel = Math.max(e.rewardedLevel, p.experienceLevel);
                s.markDirty();
            }
            if (!e.starter) {
                ServerPlayNetworking.send(p, new Payloads.Dialog("welcome"));
                List<String> starters = IdolsRow.ALL.stream().filter(r -> r.stageUnlock() == 0).map(IdolsRow::id).toList();
                ServerPlayNetworking.send(p, new Payloads.ChooseStarter(starters));
            }
            OFFERS.remove(p.getUuid());
            WaveDirector.sync(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            OFFERS.remove(handler.getPlayer().getUuid());
            Buffs.clear(handler.getPlayer().getUuid());
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.PickStarter.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            PlayerState s = get(ctx.server());
            Entry e = s.of(p.getUuid());
            IdolsRow row = IdolsRow.ALL.stream().filter(r -> r.id().equals(payload.idol()) && r.stageUnlock() == 0).findFirst().orElse(null);
            if (e.starter || row == null) return;
            e.starter = true;
            e.rewardedLevel = Math.max(e.rewardedLevel, p.experienceLevel);
            s.markDirty();
            ItemStack old = p.getInventory().getStack(0);
            p.getInventory().setStack(0, new ItemStack(HcItems.IDOLS.get(row.id())));
            if (!old.isEmpty() && !p.getInventory().insertStack(old)) p.dropItem(old, false);
            p.getInventory().selectedSlot = 0;
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket(0));
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.PickUpgrade.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            List<Payloads.Option> offer = OFFERS.remove(p.getUuid());
            if (offer == null || payload.index() < 0 || payload.index() >= offer.size()) return;
            apply(p, offer.get(payload.index()));
            PlayerState s = get(ctx.server());
            Entry e = s.of(p.getUuid());
            e.pending = Math.max(0, e.pending - 1);
            s.markDirty();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            PlayerState s = get(server);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                Entry e = s.of(p.getUuid());
                if (!e.starter) continue;
                if (p.experienceLevel > e.rewardedLevel) {
                    e.pending += p.experienceLevel - e.rewardedLevel;
                    e.rewardedLevel = p.experienceLevel;
                    s.markDirty();
                }
                if (e.pending > 0 && !OFFERS.containsKey(p.getUuid()) && p.isAlive()) {
                    List<Payloads.Option> offer = offer(p);
                    OFFERS.put(p.getUuid(), offer);
                    ServerPlayNetworking.send(p, new Payloads.LevelUp(p.experienceLevel, offer));
                }
            }
        });
    }

    /** Three distinct picks: a new idol, a weapon level, a new HoloCure item or an item level. */
    static List<Payloads.Option> offer(ServerPlayerEntity p) {
        List<Payloads.Option> pool = new ArrayList<>();
        Map<String, ItemStack> idols = new HashMap<>();
        Map<String, ItemStack> items = new HashMap<>();
        for (ItemStack st : allStacks(p)) {
            if (st.getItem() instanceof IdolItem ii) idols.putIfAbsent(ii.row().id(), st);
            if (st.getItem() instanceof HoloItem hi) items.putIfAbsent(hi.row().id(), st);
        }
        for (IdolsRow r : IdolsRow.ALL) {
            ItemStack st = idols.get(r.id());
            if (st == null) pool.add(new Payloads.Option(0, r.id(), 1));
            else if (HcItems.level(st) < 5) pool.add(new Payloads.Option(1, r.id(), HcItems.level(st) + 1));
        }
        for (ItemsRow r : ItemsRow.ALL) {
            if (!r.kind().equals("equip")) continue;
            ItemStack st = items.get(r.id());
            if (st == null) pool.add(new Payloads.Option(2, r.id(), 1));
            else if (HcItems.level(st) < 3) pool.add(new Payloads.Option(3, r.id(), HcItems.level(st) + 1));
        }
        Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
        List<Payloads.Option> out = new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
        if (out.isEmpty()) out.add(new Payloads.Option(4, "hamburger", 1));
        return out;
    }

    static void apply(ServerPlayerEntity p, Payloads.Option o) {
        switch (o.kind()) {
            case 0 -> give(p, new ItemStack(HcItems.IDOLS.get(o.id())));
            case 2 -> {
                HoloItem item = HcItems.ITEMS.get(o.id());
                ItemStack st = new ItemStack(item);
                EquipmentSlot slot = switch (item.row().slot()) {
                    case "head" -> EquipmentSlot.HEAD; case "chest" -> EquipmentSlot.CHEST;
                    case "legs" -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET;
                };
                if (p.getEquippedStack(slot).isEmpty()) p.equipStack(slot, st); else give(p, st);
            }
            case 1, 3 -> {
                Item target = o.kind() == 1 ? HcItems.IDOLS.get(o.id()) : HcItems.ITEMS.get(o.id());
                for (ItemStack st : allStacks(p))
                    if (st.isOf(target)) { st.set(HcItems.LEVEL, o.level()); break; }
            }
            default -> give(p, new ItemStack(HcItems.ITEMS.get("hamburger"), 3));
        }
    }

    static void give(ServerPlayerEntity p, ItemStack st) {
        if (!p.getInventory().insertStack(st)) p.dropItem(st, false);
    }

    static List<ItemStack> allStacks(ServerPlayerEntity p) {
        List<ItemStack> l = new ArrayList<>(p.getInventory().main);
        l.addAll(p.getInventory().armor);
        l.addAll(p.getInventory().offHand);
        return l;
    }
}
