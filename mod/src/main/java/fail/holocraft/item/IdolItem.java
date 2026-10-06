package fail.holocraft.item;

import fail.holocraft.combat.Specials;
import fail.holocraft.combat.WeaponStats;
import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.IdolsRow;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

/** An idol in the hotbar. Selected = she attacks on her own (IdolCombat); right-click = her special. */
public class IdolItem extends Item {
    private final IdolsRow row;

    public IdolItem(IdolsRow row, Settings settings) {
        super(settings);
        this.row = row;
    }

    public IdolsRow row() { return row; }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(stack)) return ActionResult.FAIL;
        if (user instanceof ServerPlayerEntity sp) {
            Specials.fire(sp, row, stack);
            user.getItemCooldownManager().set(stack, row.specialCooldownTicks());
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int level = HcItems.level(stack);
        WeaponStats w = WeaponStats.of(row, level, 0);
        tooltip.add(Text.literal(row.weaponName() + "  LV " + level).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(String.format("%.1f dmg  ·  %.1fs  ·  %.0f blocks", w.damage(), w.cooldownTicks() / 20f, w.range())).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Special: " + row.specialName() + " (" + row.specialCooldownTicks() / 20 + "s)").formatted(Formatting.LIGHT_PURPLE));
    }
}
