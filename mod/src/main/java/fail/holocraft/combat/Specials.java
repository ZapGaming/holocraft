package fail.holocraft.combat;

import fail.holocraft.registry.HcItems;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.IdolsRow;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** The special_patterns rows of systems.json, fired by right-clicking a held idol. */
public final class Specials {

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity p)) return true;
            Buffs.State b = Buffs.of(p.getUuid());
            if (p.getServerWorld().getTime() < b.reviveUntil) {
                b.reviveUntil = 0;
                p.setHealth(p.getMaxHealth());
                p.getServerWorld().spawnParticles(ParticleTypes.FLAME, p.getX(), p.getBodyY(0.5), p.getZ(), 60, 0.6, 1, 0.6, 0.08);
                return false;
            }
            return true;
        });
    }

    public static void fire(ServerPlayerEntity p, IdolsRow idol, ItemStack stack) {
        ServerWorld world = p.getServerWorld();
        long now = world.getTime();
        Buffs.State b = Buffs.of(p.getUuid());
        float base = WeaponStats.of(idol, HcItems.level(stack), 0).damage();
        int dur = idol.specialDurationTicks();
        switch (idol.special()) {
            case "slow_all" -> {
                int amp = Math.max(0, Math.round(idol.specialPower() / 0.15f) - 1);
                for (LivingEntity e : around(p, 32))
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur, amp));
                b.cooldownHalfUntil = now + dur;
            }
            case "self_damage_buff" -> { b.damageMult = idol.specialPower(); b.damageUntil = now + dur; }
            case "nova_and_buff" -> {
                nova(world, p, idol, base * idol.specialPower(), 8);
                b.damageMult = 1.5f; b.damageUntil = now + dur;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur, 1));
            }
            case "spin_ring" -> { b.spinUntil = now + dur; b.spinDamage = base * idol.specialPower(); }
            case "nova_and_revive" -> {
                nova(world, p, idol, base * idol.specialPower(), 8);
                b.reviveUntil = now + dur;
            }
            default -> {}
        }
        var snd = HcSounds.sfx(idol.specialSound());
        if (snd != null) world.playSound(null, p.getX(), p.getY(), p.getZ(), snd, SoundCategory.PLAYERS, 0.9f, 1f);
        Fx.send(world, idol.specialIcon(), p.getPos().add(0, 2.6, 0), 0, 0, 1.2f, 30);
    }

    /** Per-tick part of specials that last (spin_ring). */
    static void tick(ServerPlayerEntity p, IdolsRow idol, Buffs.State b, long now) {
        if (now < b.spinUntil && now % 10 == 0) {
            ServerWorld world = p.getServerWorld();
            for (LivingEntity e : around(p, 3.5)) {
                Attacks.hit(world, p, e, b.spinDamage);
                Vec3d k = e.getPos().subtract(p.getPos()).multiply(1, 0, 1).normalize().multiply(0.8);
                e.addVelocity(k.x, 0.25, k.z);
                e.velocityModified = true;
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + now * 0.3;
                Fx.send(world, idol.fxSprite(), p.getPos().add(Math.cos(a) * 2.2, 0.9, Math.sin(a) * 2.2), (float) a, 2f, 1f, 10);
            }
        }
    }

    static void nova(ServerWorld world, ServerPlayerEntity p, IdolsRow idol, float dmg, double r) {
        for (LivingEntity e : around(p, r)) Attacks.hit(world, p, e, dmg);
        world.spawnParticles(ParticleTypes.EXPLOSION, p.getX(), p.getBodyY(0.5), p.getZ(), 12, r / 2, 0.5, r / 2, 0);
    }

    static List<LivingEntity> around(ServerPlayerEntity p, double r) {
        return p.getServerWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e instanceof Monster && e.isAlive() && e.squaredDistanceTo(p) <= r * r);
    }

    private Specials() {}
}
