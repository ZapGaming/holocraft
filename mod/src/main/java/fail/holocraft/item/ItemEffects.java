package fail.holocraft.item;

import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.ItemsRow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import fail.holocraft.HoloCraft;

/** The item_effects rows of systems.json: what a worn HoloCure item does. */
public final class ItemEffects {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static float power(ItemsRow row, int level) {
        return row.power() + row.powerPerLevel() * (Math.max(1, level) - 1);
    }

    /** Sum of an effect over everything the player wears. */
    public static float worn(PlayerEntity p, String effect) {
        float sum = 0;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = p.getEquippedStack(s);
            if (st.getItem() instanceof HoloItem hi && hi.row().effect().equals(effect)) sum += power(hi.row(), HcItems.level(st));
        }
        return sum;
    }

    public static float expGain(PlayerEntity p) { return worn(p, "exp_gain"); }
    public static float haste(PlayerEntity p) { return Math.min(0.6f, worn(p, "haste_speed")); }
    public static float crit(PlayerEntity p) { return worn(p, "crit"); }

    /** refreshing_shield and haste_speed's movement half run every tick. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                float shield = worn(p, "refreshing_shield");
                if (shield > 0 && p.age % 300 == 0 && p.getAbsorptionAmount() < shield) p.setAbsorptionAmount(shield);
                var speed = p.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                var id = HoloCraft.id("energy_drink");
                float h = worn(p, "haste_speed");
                if (speed != null) {
                    var cur = speed.getModifier(id);
                    if (h > 0 && (cur == null || cur.value() != h)) {
                        speed.removeModifier(id);
                        speed.addTemporaryModifier(new EntityAttributeModifier(id, h, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    } else if (h <= 0 && cur != null) speed.removeModifier(id);
                }
            }
        });
    }

    private ItemEffects() {}
}
