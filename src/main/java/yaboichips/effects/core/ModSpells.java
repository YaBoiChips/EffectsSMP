package yaboichips.effects.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import yaboichips.effects.Effects;
import yaboichips.effects.common.spell.AggressiveSpell;
import yaboichips.effects.common.spell.PassiveSpell;
import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.common.spell.aggressive.ArcherSpell;
import yaboichips.effects.common.spell.aggressive.BeastTamerSpell;
import yaboichips.effects.common.spell.aggressive.ChorusSpell;
import yaboichips.effects.common.spell.aggressive.LeapSpell;
import yaboichips.effects.common.spell.aggressive.MetallurgySpell;
import yaboichips.effects.common.spell.aggressive.StealthSpell;
import yaboichips.effects.common.spell.passive.EnchanterSpell;
import yaboichips.effects.common.spell.passive.FortuneSpell;
import yaboichips.effects.common.spell.passive.HasteSpell;
import yaboichips.effects.common.spell.passive.HermitSpell;
import yaboichips.effects.common.spell.passive.LavabenderSpell;
import yaboichips.effects.common.spell.passive.LightningSpell;
import yaboichips.effects.common.spell.passive.NaturalistSpell;
import yaboichips.effects.common.spell.passive.WatcherSpell;

public class ModSpells {

    private static final Map<Identifier, PassiveSpell> PASSIVE = new LinkedHashMap<>();
    private static final Map<Identifier, AggressiveSpell> AGGRESSIVE = new LinkedHashMap<>();

    public static final PassiveSpell NATURALIST_SPELL = registerPassive("naturalist_spell", new NaturalistSpell(idOf("naturalist_spell"), iconOf("naturalist_spell")));
    public static final PassiveSpell LIGHTNING_SPELL = registerPassive("lightning_spell", new LightningSpell(idOf("lightning_spell"), iconOf("lightning_spell")));
    public static final PassiveSpell LAVABENDER_SPELL = registerPassive("lavabender_spell", new LavabenderSpell(idOf("lavabender_spell"), iconOf("lavabender_spell")));
    public static final EnchanterSpell ENCHANTER_SPELL = registerPassive("enchanter_spell", new EnchanterSpell(idOf("enchanter_spell"), iconOf("enchanter_spell")));
    public static final PassiveSpell HERMIT_SPELL = registerPassive("hermit_spell", new HermitSpell(idOf("hermit_spell"), iconOf("hermit_spell")));
    public static final FortuneSpell FORTUNE_SPELL = registerPassive("fortune_spell", new FortuneSpell(idOf("fortune_spell"), iconOf("fortune_spell")));
    public static final WatcherSpell WATCHER_SPELL = registerPassive("watcher_spell", new WatcherSpell(idOf("watcher_spell"), iconOf("watcher_spell")));

    public static final AggressiveSpell LEAP_SPELL = registerAggressive("leap_spell", new LeapSpell(idOf("leap_spell"), iconOf("leap_spell")));
    public static final AggressiveSpell METALLURGY_SPELL = registerAggressive("metallurgy_spell", new MetallurgySpell(idOf("metallurgy_spell"), iconOf("metallurgy_spell")));
    public static final AggressiveSpell STEALTH_SPELL = registerAggressive("stealth_spell", new StealthSpell(idOf("stealth_spell"), iconOf("stealth_spell")));
    public static final AggressiveSpell CHORUS_SPELL = registerAggressive("chorus_spell", new ChorusSpell(idOf("chorus_spell"), iconOf("chorus_spell")));
    public static final BeastTamerSpell BEAST_TAMER_SPELL = registerAggressive("beast_tamer_spell", new BeastTamerSpell(idOf("beast_tamer_spell"), iconOf("beast_tamer_spell")));
    public static final ArcherSpell ARCHER_SPELL = registerAggressive("archer_spell", new ArcherSpell(idOf("archer_spell"), iconOf("archer_spell")));

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
