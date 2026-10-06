package fail.holocraft.client;

import fail.holocraft.item.IdolItem;
import fail.holocraft.registry.HcItems;
import fail.holocraft.sheet.ScreensRow;
import fail.holocraft.sheet.StagesRow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** systems: holo_hud. Stage name and night timer at the top; held idol's weapon level and special charge by the hotbar. */
public final class HoloHud {
    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        if (ClientStage.active) {
            String name = StagesRow.ALL.stream().filter(s -> s.id().equals(ClientStage.stage)).map(StagesRow::display).findFirst().orElse("");
            long t = Math.max(0, mc.world.getTimeOfDay() % 24000 - 13000);
            int secs = (int) (t / 20);
            String timer = String.format("%02d:%02d", secs / 60, secs % 60);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(timer), sw / 2, 4, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(name + (ClientStage.cleared > 0 ? "  ★" + ClientStage.cleared : "")), sw / 2, 15, 0xFF9FE6FF);
        }

        ItemStack held = mc.player.getMainHandStack();
        if (held.getItem() instanceof IdolItem idol) {
            int x = sw / 2 + 96, y = sh - 24;
            HoloSprites.draw(ctx, ScreensRow.WEAPON_LEVELS.sprite(), HcItems.level(held) - 1, x, y, 24, 25);
            HoloSprites.draw(ctx, idol.row().weaponIcon(), 0, x + 2, y + 3, 20, 16);
            float cd = mc.player.getItemCooldownManager().getCooldownProgress(held, 0);
            int bx = x + 28, by = y + 6, bw = 48, bh = 9;
            HoloSprites.draw(ctx, ScreensRow.SP_BAR_BG.sprite(), 0, bx, by, bw, bh);
            int fill = Math.round(bw * (1 - cd));
            if (fill > 0) HoloSprites.drawRegion(ctx, ScreensRow.SP_BAR_FILL.sprite(), 0, bx, by, fill, bh, 0, 0, Math.round(72 * (1 - cd)), 14, 0xFFFFFFFF);
            HoloSprites.draw(ctx, ScreensRow.SP_BAR_FRAME.sprite(), 0, bx, by, bw, bh);
            HoloSprites.draw(ctx, ScreensRow.SP_CASE.sprite(), 0, bx + bw + 2, by - 3, 15, 15);
            HoloSprites.draw(ctx, idol.row().specialIcon(), 0, bx + bw + 3, by - 1, 13, 11, cd <= 0 ? 0xFFFFFFFF : 0x80FFFFFF);
        }
    }

    private HoloHud() {}
}
