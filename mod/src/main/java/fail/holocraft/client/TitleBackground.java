package fail.holocraft.client;

import fail.holocraft.sheet.ScreensRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.util.List;

/** systems: title_background. HoloCure's title stage behind every menu, with the Myth idols drifting past. */
public final class TitleBackground {
    private static final List<ScreensRow> IDOLS = List.of(ScreensRow.TITLE_AME, ScreensRow.TITLE_CALLI, ScreensRow.TITLE_GURA, ScreensRow.TITLE_INA, ScreensRow.TITLE_KIARA);

    /** False when the HoloCure art is missing, so the caller falls back to Minecraft's panorama. */
    public static boolean render(DrawContext ctx, int width, int height, float alpha) {
        HoloSprites.Info bg = HoloSprites.info(ScreensRow.TITLE_BG.sprite());
        if (bg == null) return false;
        double t = Util.getMeasuringTimeMs() / 1000.0;
        float s = Math.max(width / (float) bg.w(), height / (float) bg.h()) * 1.06f;
        int w = Math.round(bg.w() * s), h = Math.round(bg.h() * s);
        int ox = (int) ((width - w) / 2 + Math.sin(t * 0.15) * (w - width) / 2.2), oy = (height - h) / 2;
        int a = Math.round(255 * Math.max(0, Math.min(1, alpha)));
        HoloSprites.draw(ctx, ScreensRow.TITLE_BG.sprite(), 0, ox, oy, w, h, (a << 24) | 0xFFFFFF);
        int n = IDOLS.size();
        for (int i = 0; i < n; i++) {
            ScreensRow r = IDOLS.get(i);
            HoloSprites.Info info = HoloSprites.info(r.sprite());
            if (info == null) continue;
            float sc = height / 360f * 1.3f;
            int iw = Math.round(info.w() * sc), ih = Math.round(info.h() * sc);
            double phase = (t * 0.04 + i / (double) n) % 1.0;
            int x = (int) (-iw + phase * (width + iw));
            int y = height - ih - 6 + (int) (Math.sin(t * 2 + i) * 3);
            HoloSprites.draw(ctx, r.sprite(), 0, x, y, iw, ih, (Math.round(a * 0.95f) << 24) | 0xFFFFFF);
        }
        return true;
    }

    private TitleBackground() {}
}
