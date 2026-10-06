package fail.holocraft.registry;

import com.mojang.serialization.Codec;
import fail.holocraft.HoloCraft;
import fail.holocraft.item.HoloItem;
import fail.holocraft.item.IdolItem;
import fail.holocraft.sheet.IdolsRow;
import fail.holocraft.sheet.ItemsRow;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HcItems {
    /** Weapon level 1-5 on an idol, item level 1-3 on a HoloCure item. */
    public static ComponentType<Integer> LEVEL;
    public static final Map<String, IdolItem> IDOLS = new LinkedHashMap<>();
    public static final Map<String, HoloItem> ITEMS = new LinkedHashMap<>();

    public static void init() {
        LEVEL = Registry.register(Registries.DATA_COMPONENT_TYPE, HoloCraft.id("level"),
                ComponentType.<Integer>builder().codec(Codec.intRange(1, 9)).packetCodec(PacketCodecs.VAR_INT).build());

        for (IdolsRow row : IdolsRow.ALL) {
            RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, HoloCraft.id("idol_" + row.id()));
            IDOLS.put(row.id(), Registry.register(Registries.ITEM, key,
                    new IdolItem(row, new Item.Settings().registryKey(key).maxCount(1).component(LEVEL, 1))));
        }
        for (ItemsRow row : ItemsRow.ALL) {
            RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, HoloCraft.id(row.id()));
            Item.Settings s = new Item.Settings().registryKey(key);
            if (row.kind().equals("food")) {
                s.food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6f).alwaysEdible().build());
            } else {
                s.maxCount(1).component(LEVEL, 1).equippable(switch (row.slot()) {
                    case "head" -> EquipmentSlot.HEAD;
                    case "chest" -> EquipmentSlot.CHEST;
                    case "legs" -> EquipmentSlot.LEGS;
                    default -> EquipmentSlot.FEET;
                });
            }
            ITEMS.put(row.id(), Registry.register(Registries.ITEM, key, new HoloItem(row, s)));
        }

        Registry.register(Registries.ITEM_GROUP, HoloCraft.id("holocraft"), FabricItemGroup.builder()
                .displayName(Text.literal("HoloCraft"))
                .icon(() -> new ItemStack(IDOLS.values().iterator().next()))
                .entries((ctx, entries) -> {
                    IDOLS.values().forEach(entries::add);
                    ITEMS.values().forEach(entries::add);
                })
                .build());
    }

    public static int level(ItemStack stack) {
        Integer l = stack.get(LEVEL);
        return l == null ? 1 : l;
    }

    private HcItems() {}
}
