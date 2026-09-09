package yaboichips.effects.core;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import yaboichips.effects.Effects;
import yaboichips.effects.common.item.AmuletOfWillpowerItem;

public class ModItems {

    public static final Item AMULET_OF_WILLPOWER = registerItem("amulet_of_willpower", AmuletOfWillpowerItem::new, new Item.Properties());

    public static final Item Default_Book = registerItem("default_book", AmuletOfWillpowerItem::new, new Item.Properties());

    public static final Item WITHERING_BLADE = registerItem("withering_blade", Item::new, new Item.Properties().sword(ToolMaterial.COPPER,6.0f,-2.4f));

    public static final Item RAW_ANIMUM = registerItem("raw_animum", Item::new, new Item.Properties());
    public static final Item ANIMUM_INGOT = registerItem("animum_ingot", Item::new, new Item.Properties());

    public static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> itemKey = keyOf(name);
        Item item = factory.apply(properties.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    public static Item registerBlockItem(String name, Block block) {
        return registerItem(name, properties -> new BlockItem(block, properties), new Item.Properties());
    }

    private static ResourceKey<Item> keyOf(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModItems() {
        Effects.LOGGER.info("Registering items for {}", Effects.MOD_ID);
    }
}
