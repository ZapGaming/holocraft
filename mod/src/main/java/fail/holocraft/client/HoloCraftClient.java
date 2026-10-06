package fail.holocraft.client;

import fail.holocraft.HoloCraft;
import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcEntities;
import fail.holocraft.server.CoopInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public class HoloCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean ready = HoloPack.ready();
        HoloSprites.load();
        HoloSfx.load();
        HoloCraft.LOG.info("HoloCraft client: HoloCure pack {}", ready ? "ready" : "missing");

        HcEntities.FANS.values().forEach(t -> EntityRendererRegistry.register(t, SpriteEntityRenderer::new));
        HcEntities.BOSSES.values().forEach(t -> EntityRendererRegistry.register(t, SpriteEntityRenderer::new));

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Fx.ID, (p, ctx) -> FxRenderer.add(p));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Dialog.ID, (p, ctx) -> ctx.client().execute(() -> DialogOverlay.show(p.id())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ChooseStarter.ID, (p, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(new IdolSelectScreen(p.idols()))));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.LevelUp.ID, (p, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(new LevelUpScreen(p))));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Stage.ID, (p, ctx) -> {
            ClientStage.stage = p.stage();
            ClientStage.active = p.active();
            ClientStage.cleared = p.cleared();
            ClientStage.boss = p.boss();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FxRenderer.tick();
            DialogOverlay.tick();
        });
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            HoloHud.render(ctx);
            DialogOverlay.render(ctx);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(FxRenderer::render);
        CoopInfo.initClient();
        TopDownCamera.init();
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(fail.holocraft.registry.HcBlocks.PROP_ENTITY, PropRenderer::new);
    }
}
