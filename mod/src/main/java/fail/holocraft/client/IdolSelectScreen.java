package fail.holocraft.client;

import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.IdolsRow;
import fail.holocraft.sheet.ScreensRow;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** systems: idol_select. HoloCure's character select: pick the idol that starts in hotbar slot 1. */
public class IdolSelectScreen extends Screen {
    private final List<IdolsRow> idols;
    private int selected = 0;
    private int age;

    public IdolSelectScreen(List<String> ids) {
        super(Text.literal("Choose your idol"));
        this.idols = IdolsRow.ALL.stream().filter(r -> ids.contains(r.id())).toList();
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Let's go!"), b -> confirm())
                .dimensions(width / 2 - 60, height - 34, 120, 20).build());
    }

    private void confirm() {
        if (idols.isEmpty()) return;
        ClientPlayNetworking.send(new Payloads.PickStarter(idols.get(selected).id()));
        sound("ui.confirm");
        close();
    }

    private void sound(String ev) {
        var e = HcSounds.event(ev);
        if (e != null && client != null) client.getSoundManager().play(PositionedSoundInstance.master(e, 1f));
    }

    @Override
    public void tick() { age++; }

    private int cardW() { return Math.min(90, (width - 40) / Math.max(1, idols.size())); }

    private int cardX(int i) { return width / 2 - idols.size() * cardW() / 2 + i * cardW(); }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        HoloSprites.draw(ctx, ScreensRow.SELECT_BG.sprite(), 0, 0, 0, width, height);
        HoloSprites.draw(ctx, ScreensRow.SELECT_BAR.sprite(), 0, 0, 0, width, height, 0x55FFFFFF);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("CHOOSE YOUR IDOL"), width / 2, 14, 0xFFFFFFFF);
        int cw = cardW(), top = 34, ch = height - 90;
        for (int i = 0; i < idols.size(); i++) {
            IdolsRow r = idols.get(i);
            int x = cardX(i);
            boolean sel = i == selected;
            ctx.fill(x + 3, top, x + cw - 3, top + ch, sel ? 0x6624A0FF : 0x33000000);
            HoloSprites.Info info = HoloSprites.info(r.idleSprite());
            if (info != null) {
                // one scale for every idol, so they keep HoloCure's relative sizes
                float s = Math.max(1f, Math.round((ch - 40) / 64f * 2) / 2f) * (sel ? 1.0f : 0.9f);
                int w = Math.round(info.w() * s), h = Math.round(info.h() * s);
                HoloSprites.draw(ctx, r.idleSprite(), HoloSprites.frameAt(r.idleSprite(), age + delta, 6), x + (cw - w) / 2, top + ch - 34 - h, w, h);
            }
            HoloSprites.draw(ctx, r.portraitSprite(), 0, x + 6, top + 4, 24, 19);
            ctx.drawCenteredTextWithShadow(textRenderer, r.display(), x + cw / 2, top + ch - 28, sel ? 0xFFFFE680 : 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, r.weaponName(), x + cw / 2, top + ch - 16, 0xFF9FE6FF);
            if (sel) HoloSprites.draw(ctx, ScreensRow.SELECT_CURSOR.sprite(), age / 4, x + 2, top, 32, 28);
        }
        if (!idols.isEmpty()) {
            IdolsRow r = idols.get(selected);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Special: " + r.specialName()), width / 2, height - 48, 0xFFFFB0E0);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < idols.size(); i++) {
            int x = cardX(i);
            if (mx >= x && mx < x + cardW() && my >= 34 && my < height - 56) {
                if (selected == i) { confirm(); return true; }
                selected = i;
                sound("ui.confirm");
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) { selected = Math.floorMod(selected - 1, idols.size()); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) { selected = Math.floorMod(selected + 1, idols.size()); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_Z) { confirm(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean shouldPause() { return false; }
}
