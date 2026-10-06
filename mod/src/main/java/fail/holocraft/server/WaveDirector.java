package fail.holocraft.server;

import fail.holocraft.HoloCraft;
import fail.holocraft.entity.BossEntity;
import fail.holocraft.entity.HoloMob;
import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcEntities;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.BossesRow;
import fail.holocraft.sheet.FansRow;
import fail.holocraft.sheet.StagesRow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

/**
 * Every overworld night is a HoloCure stage (stages.json): the stage intro at dusk, fans by minute band
 * (fans.json minute_from/weight), bosses at their minute (bosses.json), and the last boss clears the stage and
 * brings the sunrise. Players count: spawns are per player, boss HP scales with how many are playing.
 */
public final class WaveDirector extends PersistentState {
    public static final int DUSK = 13000, DAWN = 23000, MINUTE = 1200;

    int cleared;
    boolean active;
    final Set<String> bossesSpawned = new HashSet<>();
    final Set<String> bossesDefeated = new HashSet<>();
    private static final Map<UUID, Double> SPAWN_BUDGET = new HashMap<>();

    private static final Type<WaveDirector> TYPE = new Type<>(WaveDirector::new, WaveDirector::read, DataFixTypes.LEVEL);

    static WaveDirector get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, "holocraft_stage");
    }

    private static WaveDirector read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        WaveDirector w = new WaveDirector();
        w.cleared = nbt.getInt("cleared");
        w.active = nbt.getBoolean("active");
        nbt.getList("spawned", 8).forEach(e -> w.bossesSpawned.add(e.asString()));
        nbt.getList("defeated", 8).forEach(e -> w.bossesDefeated.add(e.asString()));
        return w;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putInt("cleared", cleared);
        nbt.putBoolean("active", active);
        NbtList s = new NbtList(), d = new NbtList();
        bossesSpawned.forEach(x -> s.add(NbtString.of(x)));
        bossesDefeated.forEach(x -> d.add(NbtString.of(x)));
        nbt.put("spawned", s);
        nbt.put("defeated", d);
        return nbt;
    }

    public static boolean isNight(World world) {
        long t = world.getTimeOfDay() % 24000;
        return t >= DUSK && t < DAWN;
    }

    /** Stage for tonight: stages in order, the last one repeating. */
    static StagesRow stage(int cleared) {
        List<StagesRow> l = new ArrayList<>(StagesRow.ALL);
        l.sort(Comparator.comparingInt(StagesRow::order));
        return l.get(Math.min(cleared, l.size() - 1));
    }

    static List<BossesRow> bosses(StagesRow stage) {
        return BossesRow.ALL.stream().filter(b -> b.stage().equals(stage.id())).sorted(Comparator.comparingInt(BossesRow::minute)).toList();
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            get(world).tick(world);
        });
    }

    void tick(ServerWorld world) {
        boolean night = isNight(world);
        StagesRow stage = stage(cleared);
        if (night && !active) {
            active = true;
            bossesSpawned.clear();
            bossesDefeated.clear();
            markDirty();
            players(world).forEach(p -> ServerPlayNetworking.send(p, new Payloads.Dialog(stage.introDialog())));
            syncAll(world);
        } else if (!night && active) {
            active = false;
            for (BossesRow b : bosses(stage)) {
                EntityType<BossEntity> type = HcEntities.BOSSES.get(b.id());
                world.getEntitiesByType(type, e -> true).forEach(e -> e.discard());
            }
            markDirty();
            syncAll(world);
        }
        if (!active) return;

        int minute = (int) ((world.getTimeOfDay() % 24000 - DUSK) / MINUTE);
        List<ServerPlayerEntity> players = players(world);
        if (players.isEmpty()) return;

        for (BossesRow b : bosses(stage)) {
            if (minute >= b.minute() && bossesSpawned.add(b.id())) {
                spawnBoss(world, stage, b, players);
                markDirty();
            }
        }

        double rate = (stage.spawnPerSecond() + stage.spawnGrowth() * minute) / 20.0;
        for (ServerPlayerEntity p : players) {
            double budget = SPAWN_BUDGET.getOrDefault(p.getUuid(), 0.0) + rate;
            while (budget >= 1) {
                budget -= 1;
                int alive = world.getEntitiesByClass(HoloMob.class, new Box(p.getBlockPos()).expand(40), e -> true).size();
                if (alive >= stage.maxAlivePerPlayer()) { budget = 0; break; }
                spawnFan(world, stage, minute, p);
            }
            SPAWN_BUDGET.put(p.getUuid(), budget);
        }
    }

    private void spawnFan(ServerWorld world, StagesRow stage, int minute, ServerPlayerEntity p) {
        List<FansRow> pool = FansRow.ALL.stream().filter(f -> f.minuteFrom() <= minute).toList();
        if (pool.isEmpty()) return;
        int total = pool.stream().mapToInt(FansRow::weight).sum(), roll = world.random.nextInt(total);
        FansRow pick = pool.get(0);
        for (FansRow f : pool) { roll -= f.weight(); if (roll < 0) { pick = f; break; } }
        BlockPos pos = around(world, p, stage.radiusMin(), stage.radiusMax());
        if (pos == null) return;
        var fan = HcEntities.FANS.get(pick.id()).spawn(world, pos, SpawnReason.EVENT);
        if (fan != null) scaleHealth(fan, 1 + stage.nightScaling() * cleared);
    }

    private void spawnBoss(ServerWorld world, StagesRow stage, BossesRow b, List<ServerPlayerEntity> players) {
        ServerPlayerEntity p = players.get(world.random.nextInt(players.size()));
        BlockPos pos = around(world, p, stage.radiusMin(), stage.radiusMin() + 4);
        if (pos == null) pos = p.getBlockPos();
        BossEntity boss = HcEntities.BOSSES.get(b.id()).spawn(world, pos, SpawnReason.EVENT);
        if (boss == null) return;
        scaleHealth(boss, players.size() * (1 + stage.nightScaling() * cleared));
        var snd = HcSounds.sfx(b.arriveSound());
        for (ServerPlayerEntity q : players) {
            ServerPlayNetworking.send(q, new Payloads.Dialog(b.introDialog()));
            if (snd != null) world.playSound(null, q.getX(), q.getY(), q.getZ(), snd, SoundCategory.HOSTILE, 1f, 1f);
        }
        syncAll(world);
    }

    private static void scaleHealth(net.minecraft.entity.LivingEntity e, float mult) {
        var hp = e.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (hp == null || mult <= 1.001f) return;
        hp.addPersistentModifier(new EntityAttributeModifier(HoloCraft.id("stage_scaling"), mult - 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        e.setHealth(e.getMaxHealth());
    }

    private static BlockPos around(ServerWorld world, ServerPlayerEntity p, double rMin, double rMax) {
        for (int tries = 0; tries < 8; tries++) {
            double a = world.random.nextDouble() * Math.PI * 2, r = rMin + world.random.nextDouble() * (rMax - rMin);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * r), z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - p.getY()) > 12) continue;
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.getFluidState(pos.down()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    public static void bossDefeated(ServerWorld anyWorld, BossesRow row) {
        ServerWorld world = anyWorld.getServer().getOverworld();
        WaveDirector w = get(world);
        w.bossesDefeated.add(row.id());
        w.markDirty();
        StagesRow stage = stage(w.cleared);
        List<BossesRow> list = bosses(stage);
        boolean last = !list.isEmpty() && list.get(list.size() - 1).id().equals(row.id());
        for (ServerPlayerEntity p : players(world)) ServerPlayNetworking.send(p, new Payloads.Dialog(row.defeatDialog()));
        if (last && w.active) {
            w.cleared++;
            w.active = false;
            long day = world.getTimeOfDay() / 24000;
            world.setTimeOfDay(day * 24000 + DAWN);
            var clear = HcSounds.event("stage.clear");
            for (ServerPlayerEntity p : players(world))
                if (clear != null) world.playSound(null, p.getX(), p.getY(), p.getZ(), clear, SoundCategory.RECORDS, 1f, 1f);
            HoloCraft.LOG.info("Stage {} cleared ({} total)", stage.id(), w.cleared);
        }
        syncAll(world);
    }

    static List<ServerPlayerEntity> players(ServerWorld world) {
        return world.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    static void syncAll(ServerWorld world) {
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) sync(p);
    }

    public static void sync(ServerPlayerEntity p) {
        ServerWorld ow = p.getServer().getOverworld();
        WaveDirector w = get(ow);
        boolean boss = w.active && w.bossesSpawned.stream().anyMatch(b -> !w.bossesDefeated.contains(b));
        boolean here = p.getWorld().getRegistryKey() == World.OVERWORLD;
        ServerPlayNetworking.send(p, new Payloads.Stage(stage(w.cleared).id(), w.active && here, w.cleared, boss && here));
    }
}
