package yaboichips.effects.core;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import yaboichips.effects.Effects;
import yaboichips.effects.data.SpellBookData;

public class ModComponents {

    public static final DataComponentType<SpellBookData> SPELL_BOOK_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            keyOf("spell_book_data"),
            DataComponentType.<SpellBookData>builder()
                    .persistent(SpellBookData.CODEC)
                    .networkSynchronized(SpellBookData.STREAM_CODEC)
                    .build());

    private static ResourceKey<DataComponentType<?>> keyOf(String name) {
        return ResourceKey.create(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModComponents() {
        Effects.LOGGER.info("Registering data components for {}", Effects.MOD_ID);
    }
}
