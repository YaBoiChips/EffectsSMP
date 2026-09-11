package yaboichips.effects.core;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.MenuType;

import yaboichips.effects.Effects;
import yaboichips.effects.common.menu.SpellTableMenu;

public class ModMenus {

    /**
     * Vanilla's MenuType constructor is private in this version, so custom
     * menu types go through Fabric's ExtendedScreenHandlerType instead. The
     * table needs no actual opening data (the book's state lives on its
     * ItemStack, synced via the slot itself), so Unit is just a placeholder.
     */
    public static final MenuType<SpellTableMenu> SPELL_TABLE = Registry.register(
            BuiltInRegistries.MENU,
            keyOf("spell_table"),
            new ExtendedScreenHandlerType<>(
                    (containerId, playerInventory, data) -> new SpellTableMenu(containerId, playerInventory),
                    Unit.STREAM_CODEC));

    private static ResourceKey<MenuType<?>> keyOf(String name) {
        return ResourceKey.create(Registries.MENU, Identifier.fromNamespaceAndPath(Effects.MOD_ID, name));
    }

    public static void registerModMenus() {
        Effects.LOGGER.info("Registering menus for {}", Effects.MOD_ID);
    }
}
