package fail.holocraft.client;

import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.DialogRow;
import fail.holocraft.sheet.ScreensRow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** systems: dialog_screen. HoloCure's dialogue box at the bottom of the HUD, typing out dialog.json lines. */
public final class DialogOverlay {
    private static final Deque<DialogRow> QUEUE = new ArrayDeque<>();
    private static DialogRow current;
    private static int ticks;

    public static void show(String id) {
        for (DialogRow r : DialogRow.ALL) if (r.id().equals(id)) { QUEUE.add(r); return; }
    }

    public static boolean showing() { return current != null; }

    public static void tick() {
        if (current == null) {
            current = QUEUE.poll();
            ticks = 0;
            return;
        }
        ticks++;
        int typed = ticks * 2;
        if (typed <= current.text().length() && ticks % 3 == 0) {
            var blip = HcSounds.event("ui.text_blip");
            if (blip != null) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(blip, 1f));
        }
        if (typed > current.text().length() + 100) current = null;
    }

    public static void render(DrawContext ctx) {
        if (current == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int bw = Math.min(sw - 20, 380), bh = bw * 84 / 475;
        int x = (sw - bw) / 2, y = sh - bh - 58;
        HoloSprites.draw(ctx, ScreensRow.DIALOG_BOX.sprite(), 0, x, y, bw, bh);
        int ps = bh - 16;
        HoloSprites.draw(ctx, ScreensRow.PORTRAIT_BG.sprite(), 0, x + 10, y + 8, ps, ps);
        HoloSprites.Info pi = HoloSprites.info(current.portrait());
        if (pi != null) {
            float s = Math.min((ps - 4) / (float) pi.w(), (ps - 4) / (float) pi.h());
            int pw = Math.round(pi.w() * s), ph = Math.round(pi.h() * s);
            HoloSprites.draw(ctx, current.portrait(), (int) (ticks / 6) , x + 10 + (ps - pw) / 2, y + 8 + (ps - ph) / 2, pw, ph);
        }
        HoloSprites.draw(ctx, ScreensRow.PORTRAIT_FRAME.sprite(), 0, x + 8, y + 6, ps + 4, ps + 4);
        int tx = x + ps + 22, tw = bw - ps - 34;
        ctx.drawText(mc.textRenderer, Text.literal(current.speaker()), tx, y + 9, 0xFF7FE3FF, true);
        String shown = current.text().substring(0, Math.min(current.text().length(), ticks * 2));
        List<OrderedText> lines = mc.textRenderer.wrapLines(Text.literal(shown), tw);
        for (int i = 0; i < lines.size() && i < 4; i++) ctx.drawText(mc.textRenderer, lines.get(i), tx, y + 22 + i * 10, 0xFFFFFFFF, true);
        if (ticks * 2 >= current.text().length())
            HoloSprites.draw(ctx, ScreensRow.DIALOG_NEXT.sprite(), (ticks / 10) % 2, x + bw - 20, y + bh - 20, 10, 11);
    }

    private DialogOverlay() {}
}
