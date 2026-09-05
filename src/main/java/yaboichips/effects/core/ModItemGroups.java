package yaboichips.effects.core;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import yaboichips.effects.Effects;

public class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> EFFECTS_GROUP_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Effects.MOD_ID, "effects"));

    public static final CreativeModeTab EFFECTS_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.SPELL_TABLE))
            .title(Component.translatable("itemgroup.effects.effects"))
            .build();

    public static void registerItemGroups() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EFFECTS_GROUP_KEY, EFFECTS_GROUP);
        ItemGroupEvents.modifyEntriesEvent(EFFECTS_GROUP_KEY).register(entries ->
                BuiltInRegistries.ITEM.stream()
                        .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Effects.MOD_ID))
                        .forEach(entries::accept));
    }
}
