package yaboichips.effects.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;
import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.PassiveSpell;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.common.spell.aggressive.LeapSpell;
import yaboichips.effects.common.spell.passive.HasteSpell;
import yaboichips.effects.common.spell.passive.NaturalistSpell;

public class ModSpells {

    private static final Map<Identifier, PassiveSpell> PASSIVE = new LinkedHashMap<>();
    private static final Map<Identifier, AggressiveSpell> AGGRESSIVE = new LinkedHashMap<>();

    public static final PassiveSpell HASTE_SPELL = registerPassive("haste_spell",
            new HasteSpell(idOf("haste_spell"), iconOf("haste_spell")));

    public static final PassiveSpell NATURALIST_SPELL = registerPassive("naturalist_spell",
            new NaturalistSpell(idOf("naturalist_spell"), iconOf("naturalist_spell")));

    public static final AggressiveSpell LEAP_SPELL = registerAggressive("leap_spell",
            new LeapSpell(idOf("leap_spell"), iconOf("leap_spell")));

    private static <T extends PassiveSpell> T registerPassive(String name, T spell) {
        PASSIVE.put(idOf(name), spell);
        return spell;
    }

    private static <T extends AggressiveSpell> T registerAggressive(String name, T spell) {
        AGGRESSIVE.put(idOf(name), spell);
        return spell;
    }

    private static Identifier idOf(String name) {
        return Identifier.fromNamespaceAndPath(Effects.MOD_ID, name);
    }

    private static Identifier iconOf(String name) {
        return Identifier.fromNamespaceAndPath(Effects.MOD_ID, "textures/gui/spell/" + name + ".png");
    }

    public static Collection<PassiveSpell> passiveSpells() {
        return PASSIVE.values();
    }

    public static Collection<AggressiveSpell> aggressiveSpells() {
        return AGGRESSIVE.values();
    }

    public static Spell get(Identifier spellId) {
        Spell spell = PASSIVE.get(spellId);
        return spell != null ? spell : AGGRESSIVE.get(spellId);
    }

    public static void registerModSpells() {
        Effects.LOGGER.info("Registering spells for {}", Effects.MOD_ID);
    }
}
