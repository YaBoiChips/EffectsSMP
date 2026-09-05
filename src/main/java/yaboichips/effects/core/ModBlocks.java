package yaboichips.effects.core;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import yaboichips.effects.Effects;
import yaboichips.effects.common.block.SpellTableBlock;

public class ModBlocks {

    public static final Block SPELL_TABLE = registerBlock("spell_table", SpellTableBlock::new, BlockBehaviour.Properties.of().strength(3.5f));

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = keyOf(name);
        Block block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        ModItems.registerBlockItem(name, block);
        return block;
    }

    private static ResourceKey<Block> keyOf(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModBlocks() {
        Effects.LOGGER.info("Registering blocks for {}", Effects.MOD_ID);
    }
}
