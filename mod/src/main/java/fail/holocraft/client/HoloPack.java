package fail.holocraft.client;

import net.minecraft.resource.*;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Consumer;

/** Resource pack provider for the generated HoloCraft pack: required (always on), on top of everything else. */
public final class HoloPack implements ResourcePackProvider {
    public static final String ID = "holocraft:holocure";
    private static Boolean ready;

    public static boolean ready() {
        if (ready == null) ready = HoloCureImport.ensure();
        return ready;
    }

    @Override
    public void register(Consumer<ResourcePackProfile> adder) {
        if (!ready() || !Files.isRegularFile(HoloCureImport.packDir().resolve("pack.mcmeta"))) return;
        ResourcePackInfo info = new ResourcePackInfo(ID, Text.literal("HoloCraft (from your HoloCure)"), ResourcePackSource.BUILTIN, Optional.empty());
        ResourcePackProfile profile = ResourcePackProfile.create(info,
                new DirectoryResourcePack.DirectoryBackedFactory(HoloCureImport.packDir()),
                ResourceType.CLIENT_RESOURCES,
                new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, true));
        if (profile != null) adder.accept(profile);
    }
}
