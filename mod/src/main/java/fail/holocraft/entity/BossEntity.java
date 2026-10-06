package fail.holocraft.entity;

import fail.holocraft.registry.HcEntities;
import fail.holocraft.server.WaveDirector;
import fail.holocraft.sheet.BossesRow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A stage boss: its bosses.json row sets stats and which move it uses (systems: jump_slam, charge). */
public class BossEntity extends HoloMob {
    private final BossesRow row;
    private final ServerBossBar bar;
    private int moveTimer = 100;
    private int windup = 0;
    private Vec3d chargeDir = Vec3d.ZERO;

    public BossEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.row = HcEntities.BOSS_ROWS.get(type);
        this.bar = new ServerBossBar(Text.literal(row.display()), BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
        this.experiencePoints = row.exp();
    }

    public BossesRow row() { return row; }
    @Override public String sprite() { return row.sprite(); }
    @Override public float spriteSize() { return row.scale(); }
    @Override public int depthPx() { return row.depthPx(); }
    @Override protected int baseExp() { return row.exp(); }
    @Override protected String hurtSoundName() { return "snd_hit3"; }
    /** A boss leaves at dawn only if nobody beat it; WaveDirector handles that so the stage can be failed cleanly. */
    @Override protected boolean leavesAtDawn() { return false; }
    @Override public boolean cannotDespawn() { return true; }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bar.removePlayer(player);
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        bar.setPercent(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target == null) return;
        if (windup > 0) {
            windup--;
            getNavigation().stop();
            if (windup == 0) release(world, target);
            return;
        }
        if (--moveTimer > 0) return;
        switch (row.move()) {
            case "jump_slam" -> { moveTimer = 120; windup = 15; }
            case "charge" -> {
                moveTimer = 100; windup = 20;
                chargeDir = target.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
            }
            default -> moveTimer = 100;
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 0.6f, 1.6f);
    }

    private void release(ServerWorld world, LivingEntity target) {
        if (row.move().equals("jump_slam")) {
            Vec3d d = target.getPos().subtract(getPos());
            setVelocity(d.x * 0.18, 0.9, d.z * 0.18);
            velocityModified = true;
            slamPending = true;
        } else if (row.move().equals("charge")) {
            setVelocity(chargeDir.x * 1.4, 0.1, chargeDir.z * 1.4);
            velocityModified = true;
            chargeTicks = 14;
        }
    }

    private boolean slamPending;
    private int chargeTicks;

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        if (slamPending && isOnGround() && getVelocity().y <= 0) {
            slamPending = false;
            world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 6, 1.5, 0.2, 1.5, 0);
            hitAround(world, 3.0, row.damage() * 1.5f);
        }
        if (chargeTicks > 0) {
            chargeTicks--;
            setVelocity(chargeDir.x * 1.4, getVelocity().y, chargeDir.z * 1.4);
            velocityModified = true;
            hitAround(world, 1.6, row.damage());
        }
    }

    private void hitAround(ServerWorld world, double r, float dmg) {
        for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(r), Entity::isAlive))
            p.damage(world, getDamageSources().mobAttack(this), dmg);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (getWorld() instanceof ServerWorld world) WaveDirector.bossDefeated(world, row);
        bar.clearPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.clearPlayers();
        super.remove(reason);
    }
}
