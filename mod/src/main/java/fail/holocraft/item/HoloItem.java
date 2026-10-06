package fail.holocraft.item;

import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.ItemsRow;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** A HoloCure item: worn in an armour slot for its passive, or eaten (food). */
public class HoloItem extends Item {
    private final ItemsRow row;

    public HoloItem(ItemsRow row, Settings settings) {
        super(settings);
        this.row = row;
    }

    public ItemsRow row() { return row; }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && row.effect().equals("heal_pct")) user.heal(user.getMaxHealth() * row.power());
        return super.finishUsing(stack, world, user);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int level = HcItems.level(stack);
        float p = ItemEffects.power(row, level);
        String what = switch (row.effect()) {
            case "exp_gain" -> String.format("+%d%% EXP", Math.round(p * 100));
            case "refreshing_shield" -> String.format("%.0f HP shield, refreshes every 15s", p);
            case "haste_speed" -> String.format("-%d%% idol cooldowns, +%d%% speed", Math.round(p * 100), Math.round(p * 100));
            case "crit" -> String.format("%d%% crit chance (1.5x)", Math.round(p * 100));
            case "heal_pct" -> String.format("Heals %d%% HP", Math.round(p * 100));
            default -> row.effect();
        };
        if (row.kind().equals("equip")) tooltip.add(Text.literal("LV " + level + "  ·  wear on " + row.slot()).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(what).formatted(Formatting.GRAY));
    }
}
