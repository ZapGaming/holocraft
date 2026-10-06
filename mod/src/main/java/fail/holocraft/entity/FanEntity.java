package fail.holocraft.entity;

import fail.holocraft.registry.HcEntities;
import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.FansRow;
import fail.holocraft.sheet.ItemsRow;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/** One swarm enemy; every stat comes from its fans.json row. */
public class FanEntity extends HoloMob {
    private final FansRow row;

    public FanEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.row = HcEntities.FAN_ROWS.get(type);
    }

    public FansRow row() { return row; }
    @Override public String sprite() { return row.sprite(); }
    @Override public float spriteSize() { return row.scale(); }
    @Override public int depthPx() { return row.depthPx(); }
    @Override protected int baseExp() { return row.exp(); }
    @Override protected String hurtSoundName() { return row.hurtSound(); }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropLoot(world, source, causedByPlayer);
        if (!causedByPlayer) return;
        for (ItemsRow item : ItemsRow.ALL)
            if (item.dropChance() > 0 && random.nextFloat() < item.dropChance())
                dropStack(world, new ItemStack(HcItems.ITEMS.get(item.id())));
    }
}
