package fail.holocraft.registry;

import fail.holocraft.HoloCraft;
import fail.holocraft.sheet.PropsRow;
import fail.holocraft.sheet.StagesRow;
import fail.holocraft.world.PropBlock;
import fail.holocraft.world.PropBlockEntity;
import fail.holocraft.world.StageChunkGenerator;
import fail.holocraft.world.StageGroundBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HcBlocks {
    public static final Map<String, StageGroundBlock> GROUND = new LinkedHashMap<>();
    public static final Map<String, PropBlock> PROPS = new LinkedHashMap<>();
    public static BlockEntityType<PropBlockEntity> PROP_ENTITY;

    public static void init() {
        for (StagesRow s : StagesRow.ALL) {
            String id = s.id() + "_ground";
            GROUND.put(s.id(), register(id, new StageGroundBlock(s, settings(id).strength(0.6f).sounds(BlockSoundGroup.GRASS).mapColor(MapColor.PALE_GREEN))));
        }
        for (PropsRow p : PropsRow.ALL) {
            String id = "prop_" + p.id();
            AbstractBlock.Settings st = settings(id).strength(p.solid() ? 1.5f : 0.1f).nonOpaque().noBlockBreakParticles();
            if (!p.solid()) st = st.noCollision().sounds(BlockSoundGroup.GRASS);
            PROPS.put(p.id(), register(id, new PropBlock(p, st)));
        }
        PROP_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, HoloCraft.id("prop"),
                FabricBlockEntityTypeBuilder.create(PropBlockEntity::new, PROPS.values().toArray(new Block[0])).build());
        Registry.register(Registries.CHUNK_GENERATOR, HoloCraft.id("stage"), StageChunkGenerator.CODEC);
    }

    private static AbstractBlock.Settings settings(String id) {
        return AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, HoloCraft.id(id)));
    }

    private static <B extends Block> B register(String id, B block) {
        Registry.register(Registries.BLOCK, HoloCraft.id(id), block);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, HoloCraft.id(id));
        Registry.register(Registries.ITEM, key, new BlockItem(block, new Item.Settings().registryKey(key).useBlockPrefixedTranslationKey()));
        return block;
    }

    private HcBlocks() {}
}
