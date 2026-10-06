package fail.holocraft.entity;

import fail.holocraft.item.ItemEffects;
import fail.holocraft.registry.HcSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** A HoloCure enemy living in a Minecraft world: walks straight at the nearest player, drawn as a HoloCure sprite. */
public abstract class HoloMob extends HostileEntity {
    protected HoloMob(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public abstract String sprite();
    public abstract float spriteSize();
    /** Thickness of the 3D pixel model, in HoloCure pixels (depth_px). */
    public abstract int depthPx();
    protected abstract int baseExp();
    protected abstract String hurtSoundName();
    /** Fans vanish at sunrise; bosses decide for themselves. */
    protected boolean leavesAtDawn() { return true; }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld() instanceof ServerWorld sw && leavesAtDawn() && !fail.holocraft.server.WaveDirector.isNight(sw) && age % 20 == (getId() & 15)
                && random.nextInt(4) == 0) {
            sw.spawnParticles(ParticleTypes.POOF, getX(), getBodyY(0.5), getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            discard();
        }
    }

    @Override
    protected int getExperienceToDrop(ServerWorld world) {
        float mult = 1f;
        if (getAttacker() instanceof PlayerEntity p) mult += ItemEffects.expGain(p);
        return Math.round(baseExp() * mult);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) { return HcSounds.sfx(hurtSoundName()); }

    @Override
    protected @Nullable SoundEvent getAmbientSound() { return null; }

    @Override
    protected @Nullable SoundEvent getDeathSound() { return null; }

    @Override
    protected boolean isAffectedByDaylight() { return false; }
}
