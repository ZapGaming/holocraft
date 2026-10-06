package fail.holocraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

/** systems: missing_holocure_screen. HoloCraft draws everything from the player's HoloCure; say so once if it is missing. */
public class MissingHoloCureScreen extends Screen {
    private static boolean shown;
    private final Screen parent;

    public static void maybeShow(Screen parent) {
        if (shown || HoloSprites.available()) return;
        shown = true;
        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new MissingHoloCureScreen(parent)));
    }

    MissingHoloCureScreen(Screen parent) {
        super(Text.literal("HoloCure needed"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Get HoloCure (free on Steam)"),
                b -> Util.getOperatingSystem().open("steam://install/" + HoloCureLocator.STEAM_APP)).dimensions(width / 2 - 100, height / 2 + 30, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Continue without it"), b -> close()).dimensions(width / 2 - 100, height / 2 + 54, 200, 20).build());
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        String msg = "HoloCraft draws its idols, fans, menus, fonts and music from your own copy of HoloCure - Save the Fans!, "
                + "and couldn't find it on this PC. Install it from Steam (it's free), then restart HoloCraft.";
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(msg), Math.min(320, width - 40));
        int y = height / 2 - 20 - lines.size() * 10;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, y - 16, 0xFFFF9FD0);
        for (OrderedText l : lines) { ctx.drawCenteredTextWithShadow(textRenderer, l, width / 2, y, 0xFFFFFFFF); y += 10; }
    }
}
