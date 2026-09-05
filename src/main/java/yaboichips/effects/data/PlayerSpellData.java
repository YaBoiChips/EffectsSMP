package yaboichips.effects.data;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.common.spell.SpellType;

public record PlayerSpellData(
        int spellPoints,
        Map<Identifier, Integer> spellLevels,
        Optional<Identifier> equippedPassive,
        Optional<Identifier> equippedAggressive) {

    public static final PlayerSpellData DEFAULT = new PlayerSpellData(3, Map.of(), Optional.empty(), Optional.empty());

    public static final Codec<PlayerSpellData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("spell_points").forGetter(PlayerSpellData::spellPoints),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("spell_levels").forGetter(PlayerSpellData::spellLevels),
            Identifier.CODEC.optionalFieldOf("equipped_passive").forGetter(PlayerSpellData::equippedPassive),
            Identifier.CODEC.optionalFieldOf("equipped_aggressive").forGetter(PlayerSpellData::equippedAggressive)
    ).apply(instance, PlayerSpellData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PlayerSpellData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public int levelOf(Identifier spellId) {
        return spellLevels.getOrDefault(spellId, 0);
    }

    public boolean isUnlocked(Identifier spellId) {
        return levelOf(spellId) > 0;
    }

    public boolean isEquipped(Identifier spellId) {
        return equippedPassive.map(spellId::equals).orElse(false)
                || equippedAggressive.map(spellId::equals).orElse(false);
    }

    public PlayerSpellData withSpellPoints(int newSpellPoints) {
        return new PlayerSpellData(newSpellPoints, spellLevels, equippedPassive, equippedAggressive);
    }

    public PlayerSpellData tryUnlock(Spell spell) {
        if (spell == null || isUnlocked(spell.id()) || spellPoints < 1) {
            return this;
        }
        Map<Identifier, Integer> newLevels = new java.util.HashMap<>(spellLevels);
        newLevels.put(spell.id(), 1);
        return new PlayerSpellData(spellPoints - 1, newLevels, equippedPassive, equippedAggressive);
    }

    public PlayerSpellData tryUpgrade(Spell spell) {
        if (spell == null) {
            return this;
        }
        int currentLevel = levelOf(spell.id());
        if (currentLevel <= 0 || currentLevel >= spell.maxLevel() || spellPoints < 1) {
            return this;
        }
        Map<Identifier, Integer> newLevels = new java.util.HashMap<>(spellLevels);
        newLevels.put(spell.id(), currentLevel + 1);
        return new PlayerSpellData(spellPoints - 1, newLevels, equippedPassive, equippedAggressive);
    }

    public PlayerSpellData withEquipped(Spell spell) {
        if (spell == null || !isUnlocked(spell.id())) {
            return this;
        }
        if (spell.type() == SpellType.PASSIVE) {
            return new PlayerSpellData(spellPoints, spellLevels, Optional.of(spell.id()), equippedAggressive);
        }
        return new PlayerSpellData(spellPoints, spellLevels, equippedPassive, Optional.of(spell.id()));
    }
}
