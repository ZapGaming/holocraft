package fail.holocraft.combat;

import fail.holocraft.item.IdolItem;
import fail.holocraft.item.ItemEffects;
import fail.holocraft.registry.HcItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/** The selected hotbar idol attacks on her own, on her weapon's cooldown, whenever something hostile is in range. */
public final class IdolCombat {
    public static void init() {
        ItemEffects.init();
        Specials.init();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!p.isAlive() || p.isSpectator()) continue;
                ItemStack held = p.getMainHandStack();
                if (!(held.getItem() instanceof IdolItem idol)) continue;
                ServerWorld world = p.getServerWorld();
                Buffs.State b = Buffs.of(p.getUuid());
                long now = world.getTime();
                Specials.tick(p, idol.row(), b, now);
                if (now < b.nextAttack) continue;
                WeaponStats w = WeaponStats.of(idol.row(), HcItems.level(held), ItemEffects.haste(p));
                List<LivingEntity> targets = targets(p, w.range());
                if (targets.isEmpty()) continue;
                int cd = w.cooldownTicks();
                if (now < b.cooldownHalfUntil) cd = Math.max(4, cd / 2);
                b.nextAttack = now + cd;
                Attacks.perform(p, idol.row(), w, targets, b, now);
            }
        });
    }

    /** Hostile things in range, nearest first. */
    public static List<LivingEntity> targets(ServerPlayerEntity p, double range) {
        List<LivingEntity> l = p.getServerWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range),
                e -> e instanceof Monster && e.isAlive() && e.squaredDistanceTo(p) <= range * range);
        l.sort((a, c) -> Double.compare(a.squaredDistanceTo(p), c.squaredDistanceTo(p)));
        return l;
    }

    private IdolCombat() {}
}
