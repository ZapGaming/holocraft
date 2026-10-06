package fail.holocraft;

import fail.holocraft.combat.IdolCombat;
import fail.holocraft.net.Payloads;
import fail.holocraft.registry.HcEntities;
import fail.holocraft.registry.HcItems;
import fail.holocraft.registry.HcSounds;
import fail.holocraft.server.PlayerState;
import fail.holocraft.server.WaveDirector;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HoloCraft implements ModInitializer {
    public static final String MOD_ID = "holocraft";
    public static final String VERSION = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    public static final Logger LOG = LoggerFactory.getLogger("HoloCraft");

    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override
    public void onInitialize() {
        HcSounds.init();
        HcItems.init();
        fail.holocraft.registry.HcBlocks.init();
        HcEntities.init();
        Payloads.init();
        PlayerState.init();
        IdolCombat.init();
        WaveDirector.init();
        LOG.info("HoloCraft {} ready: {} idols, {} fans, {} bosses", VERSION,
                fail.holocraft.sheet.IdolsRow.ALL.size(), fail.holocraft.sheet.FansRow.ALL.size(), fail.holocraft.sheet.BossesRow.ALL.size());
    }
}
