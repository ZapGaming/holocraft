package fail.holocraft.client;

import fail.holocraft.item.ItemEffects;
import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.sheet.IdolsRow;
import fail.holocraft.sheet.ItemsRow;
import fail.holocraft.sheet.ScreensRow;
import fail.holocraft.sheet.UpgradesRow;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** systems: level_up_screen. HoloCure's LEVEL UP! panel: three upgrade windows, pick one. */
public class LevelUpScreen extends Screen {
    private final Payloads.LevelUp offer;
    private int selected;
    private int age;

    public LevelUpScreen(Payloads.LevelUp offer) {
        super(Text.literal("Level up!"));
        this.offer = offer;
    }

    record Card(String icon, String name, String tag, String desc) {}

    Card card(Payloads.Option o) {
        switch (o.kind()) {
            case 0, 1 -> {
                IdolsRow r = IdolsRow.ALL.stream().filter(x -> x.id().equals(o.id())).findFirst().orElseThrow();
                if (o.kind() == 0)
                    return new Card(r.portraitSprite(), r.display(), "NEW!", "Joins your hotbar. " + r.weaponName() + "; right-click for " + r.specialName() + ".");
                UpgradesRow u = UpgradesRow.ALL.stream().filter(x -> x.level() == o.level()).findFirst().orElse(null);
                String d = u == null ? "" : String.format("%s: x%.1f damage, %d%% faster, %d hits.", r.weaponName(), u.damageMult(),
                        Math.round((1 - u.cooldownMult()) * 100), 1 + u.extraHits());
                return new Card(r.weaponIcon(), r.weaponName(), "LV " + o.level(), d);
            }
            case 2, 3 -> {
                ItemsRow r = ItemsRow.ALL.stream().filter(x -> x.id().equals(o.id())).findFirst().orElseThrow();
                float p = ItemEffects.power(r, o.level());
                String d = switch (r.effect()) {
                    case "exp_gain" -> String.format("+%d%% EXP.", Math.round(p * 100));
                    case "refreshing_shield" -> String.format("A %.0f HP shield that refreshes every 15 seconds.", p);
                    case "haste_speed" -> String.format("Idols attack %d%% faster, you move %d%% faster.", Math.round(p * 100), Math.round(p * 100));
                    case "crit" -> String.format("%d%% chance for 1.5x damage.", Math.round(p * 100));
                    default -> r.effect();
                };
                return new Card(r.sprite(), r.display(), o.kind() == 2 ? "NEW!" : "LV " + o.level(), d + " Wear it: " + r.slot() + ".");
            }
            default -> {
                ItemsRow r = ItemsRow.HAMBURGER;
                return new Card(r.sprite(), r.display(), "x3", "Heals 20% HP when eaten.");
            }
        }
    }

    @Override
    public void tick() { age++; }

    private int winW() { return Math.min(300, width - 40); }
    private int winH() { return winW() * 68 / 386; }
    private int winY(int i) { return height / 2 - (offer.options().size() * (winH() + 6)) / 2 + i * (winH() + 6) + 10; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int ww = winW(), wh = winH(), x = (width - ww) / 2;
        int ty = winY(0) - 36;
        HoloSprites.draw(ctx, ScreensRow.UPGRADE_TITLE.sprite(), 0, width / 2 - 47, ty, 94, 20);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("LV " + offer.level()), width / 2, ty + 22, 0xFFFFE066);
        List<Payloads.Option> opts = offer.options();
        for (int i = 0; i < opts.size(); i++) {
            Card c = card(opts.get(i));
            int y = winY(i);
            boolean sel = i == selected;
            HoloSprites.draw(ctx, sel ? ScreensRow.UPGRADE_WINDOW_SELECTED.sprite() : ScreensRow.UPGRADE_WINDOW.sprite(), sel ? (age / 8) % 2 : 0, x, y, ww, wh);
            int is = wh - 12;
            HoloSprites.draw(ctx, ScreensRow.SKILL_ICON_BACK.sprite(), 0, x + 6, y + 6, is, is);
            HoloSprites.Info ii = HoloSprites.info(c.icon());
            if (ii != null) {
                float s = Math.min((is - 4) / (float) ii.w(), (is - 4) / (float) ii.h());
                int iw = Math.round(ii.w() * s), ih = Math.round(ii.h() * s);
                HoloSprites.draw(ctx, c.icon(), 0, x + 6 + (is - iw) / 2, y + 6 + (is - ih) / 2, iw, ih);
            }
            int tx = x + is + 14;
            ctx.drawText(textRenderer, Text.literal(c.name()), tx, y + 2, 0xFFFFFFFF, true);
            ctx.drawText(textRenderer, Text.literal(c.tag()), x + ww - 8 - textRenderer.getWidth(c.tag()), y + 2, c.tag().startsWith("NEW") ? 0xFFFFE066 : 0xFF7FE3FF, true);
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(c.desc()), ww - is - 24);
            for (int l = 0; l < lines.size() && l < 3; l++) ctx.drawText(textRenderer, lines.get(l), tx, y + 17 + l * 9, 0xFFD8D8E8, false);
        }
    }

    private void pick(int i) {
        ClientPlayNetworking.send(new Payloads.PickUpgrade(i));
        var e = HcSounds.event("ui.confirm");
        if (e != null && client != null) client.getSoundManager().play(PositionedSoundInstance.master(e, 1f));
        close();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (width - winW()) / 2;
        for (int i = 0; i < offer.options().size(); i++) {
            int y = winY(i);
            if (mx >= x && mx < x + winW() && my >= y && my < y + winH()) { pick(i); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        int x = (width - winW()) / 2;
        for (int i = 0; i < offer.options().size(); i++) {
            int y = winY(i);
            if (mx >= x && mx < x + winW() && my >= y && my < y + winH()) selected = i;
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int n = offer.options().size();
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_W) { selected = Math.floorMod(selected - 1, n); return true; }
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_S) { selected = Math.floorMod(selected + 1, n); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_Z) { pick(selected); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean shouldPause() { return true; }
}
