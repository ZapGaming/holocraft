package fail.holocraft.combat;

import fail.holocraft.item.ItemEffects;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.IdolsRow;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The attack_patterns rows of systems.json. */
public final class Attacks {

    public static void perform(ServerPlayerEntity p, IdolsRow idol, WeaponStats w, List<LivingEntity> targets, Buffs.State b, long now) {
        ServerWorld world = p.getServerWorld();
        float dmg = w.damage() * (now < b.damageUntil ? b.damageMult : 1f);
        switch (idol.attack()) {
            case "projectile_burst" -> projectileBurst(world, p, idol, w, targets, dmg);
            case "arc_front" -> arcFront(world, p, idol, w, targets, dmg);
            case "thrust_line" -> thrustLine(world, p, idol, w, targets, dmg);
            default -> { return; }
        }
        var snd = HcSounds.sfx(idol.attackSound());
        if (snd != null) world.playSound(null, p.getX(), p.getY(), p.getZ(), snd, SoundCategory.PLAYERS, 0.55f, 1f);
    }

    /** Fires 1+extra_hits shots at the nearest targets; each pierces through others on its line. */
    static void projectileBurst(ServerWorld world, ServerPlayerEntity p, IdolsRow idol, WeaponStats w, List<LivingEntity> targets, float dmg) {
        Vec3d from = p.getEyePos().subtract(0, 0.35, 0);
        for (int i = 0; i < w.hits(); i++) {
            LivingEntity t = targets.get(i % targets.size());
            Vec3d to = t.getPos().add(0, t.getHeight() * 0.5, 0);
            Vec3d dir = to.subtract(from).normalize();
            Vec3d end = from.add(dir.multiply(w.range()));
            Set<LivingEntity> hit = new HashSet<>();
            for (LivingEntity e : targets) {
                if (hit.size() >= w.pierce()) break;
                if (e == t || distToSegment(e.getPos().add(0, e.getHeight() * 0.5, 0), from, end) < 0.7) hit.add(e);
            }
            hit.add(t);
            for (LivingEntity e : hit) hit(world, p, e, dmg);
            Fx.send(world, idol.fxSprite(), from, yaw(dir), (float) from.distanceTo(to), 0.6f, 6);
        }
    }

    /** Half-circle sweep in front (and behind, with extra hits). */
    static void arcFront(ServerWorld world, ServerPlayerEntity p, IdolsRow idol, WeaponStats w, List<LivingEntity> targets, float dmg) {
        Vec3d look = flat(targets.get(0).getPos().subtract(p.getPos()));
        for (int i = 0; i < w.hits(); i++) {
            Vec3d dir = i == 0 ? look : look.rotateY((float) Math.PI * i / w.hits() * 2);
            for (LivingEntity e : targets) {
                Vec3d to = flat(e.getPos().subtract(p.getPos()));
                if (to.lengthSquared() < 1e-4 || to.normalize().dotProduct(dir) >= 0) {
                    if (e.squaredDistanceTo(p) <= w.range() * w.range()) hit(world, p, e, dmg);
                }
            }
            Fx.send(world, idol.fxSprite(), p.getPos().add(dir.multiply(w.range() * 0.5)).add(0, 0.9, 0), yaw(dir), w.range(), w.range() / 2f, 10);
        }
    }

    /** Straight stab toward the nearest target; extra hits fan out ±20° per extra. */
    static void thrustLine(ServerWorld world, ServerPlayerEntity p, IdolsRow idol, WeaponStats w, List<LivingEntity> targets, float dmg) {
        Vec3d base = flat(targets.get(0).getPos().subtract(p.getPos()));
        Vec3d from = p.getPos().add(0, 0.9, 0);
        for (int i = 0; i < w.hits(); i++) {
            float off = (float) Math.toRadians(20 * ((i + 1) / 2) * (i % 2 == 0 ? 1 : -1));
            Vec3d dir = base.rotateY(off);
            Vec3d end = from.add(dir.multiply(w.range()));
            for (LivingEntity e : targets)
                if (distToSegment(e.getPos().add(0, e.getHeight() * 0.5, 0), from, end) < 1.0) hit(world, p, e, dmg);
            Fx.send(world, idol.fxSprite(), from.add(dir.multiply(w.range() * 0.5)), yaw(dir), w.range(), 1f, 8);
        }
    }

    public static void hit(ServerWorld world, ServerPlayerEntity p, LivingEntity e, float dmg) {
        if (!e.isAlive()) return;
        if (world.random.nextFloat() < ItemEffects.crit(p)) dmg *= 1.5f;
        e.timeUntilRegen = 0;
        e.damage(world, world.getDamageSources().playerAttack(p), dmg);
    }

    static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0, v.z);
        return f.lengthSquared() < 1e-6 ? new Vec3d(1, 0, 0) : f.normalize();
    }

    static float yaw(Vec3d dir) { return (float) MathHelper.atan2(dir.z, dir.x); }

    static double distToSegment(Vec3d pt, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double t = MathHelper.clamp(pt.subtract(a).dotProduct(ab) / Math.max(1e-6, ab.lengthSquared()), 0, 1);
        return pt.distanceTo(a.add(ab.multiply(t)));
    }

    private Attacks() {}
}
