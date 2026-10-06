package fail.holocraft.registry;

import fail.holocraft.HoloCraft;
import fail.holocraft.entity.BossEntity;
import fail.holocraft.entity.FanEntity;
import fail.holocraft.sheet.BossesRow;
import fail.holocraft.sheet.FansRow;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HcEntities {
    public static final Map<String, EntityType<FanEntity>> FANS = new LinkedHashMap<>();
    public static final Map<String, EntityType<BossEntity>> BOSSES = new LinkedHashMap<>();
    public static final Map<EntityType<?>, FansRow> FAN_ROWS = new LinkedHashMap<>();
    public static final Map<EntityType<?>, BossesRow> BOSS_ROWS = new LinkedHashMap<>();

    public static void init() {
        for (FansRow row : FansRow.ALL) {
            RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, HoloCraft.id(row.id()));
            EntityType<FanEntity> type = Registry.register(Registries.ENTITY_TYPE, key,
                    EntityType.Builder.<FanEntity>create(FanEntity::new, SpawnGroup.MONSTER)
                            .dimensions(row.hitboxW(), row.hitboxH()).maxTrackingRange(10).build(key));
            FANS.put(row.id(), type);
            FAN_ROWS.put(type, row);
            FabricDefaultAttributeRegistry.register(type, HostileEntity.createHostileAttributes()
                    .add(EntityAttributes.MAX_HEALTH, row.hp())
                    .add(EntityAttributes.MOVEMENT_SPEED, row.speed())
                    .add(EntityAttributes.ATTACK_DAMAGE, row.damage())
                    .add(EntityAttributes.FOLLOW_RANGE, 48.0));
        }
        for (BossesRow row : BossesRow.ALL) {
            RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, HoloCraft.id(row.id()));
            EntityType<BossEntity> type = Registry.register(Registries.ENTITY_TYPE, key,
                    EntityType.Builder.<BossEntity>create(BossEntity::new, SpawnGroup.MONSTER)
                            .dimensions(row.hitboxW(), row.hitboxH()).maxTrackingRange(16).build(key));
            BOSSES.put(row.id(), type);
            BOSS_ROWS.put(type, row);
            FabricDefaultAttributeRegistry.register(type, HostileEntity.createHostileAttributes()
                    .add(EntityAttributes.MAX_HEALTH, row.hp())
                    .add(EntityAttributes.MOVEMENT_SPEED, row.speed())
                    .add(EntityAttributes.ATTACK_DAMAGE, row.damage())
                    .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9)
                    .add(EntityAttributes.FOLLOW_RANGE, 64.0));
        }
    }

    private HcEntities() {}
}
