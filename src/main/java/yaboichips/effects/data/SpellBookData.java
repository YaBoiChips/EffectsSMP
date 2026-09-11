package yaboichips.effects.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import yaboichips.effects.common.spell.Spell;
import yaboichips.effects.common.spell.SpellType;

public record SpellBookData(
        Optional<UUID> owner,
        Map<Identifier, Integer> spellLevels,
        Optional<Identifier> equippedPassive,
        Optional<Identifier> equippedAggressive,
        int investedPoints) {

    public static final SpellBookData DEFAULT =
            new SpellBookData(Optional.empty(), Map.of(), Optional.empty(), Optional.empty(), 0);

    public static final Codec<SpellBookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(SpellBookData::owner),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("spell_levels").forGetter(SpellBookData::spellLevels),
            Identifier.CODEC.optionalFieldOf("equipped_passive").forGetter(SpellBookData::equippedPassive),
            Identifier.CODEC.optionalFieldOf("equipped_aggressive").forGetter(SpellBookData::equippedAggressive),
            Codec.INT.fieldOf("invested_points").forGetter(SpellBookData::investedPoints)
    ).apply(instance, SpellBookData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SpellBookData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public int levelOf(Identifier spellId) {
        return spellLevels.getOrDefault(spellId, 0);
    }

    public boolean isUnlocked(Identifier spellId) {
        return levelOf(spellId) > 0;
    }

    public boolean isOwner(UUID playerId) {
        return owner.map(playerId::equals).orElse(false);
    }

    /**
     * True only once someone else has already bound this book - an unbound
     * book (owner empty) is fair game for anyone, since nothing has been
     * invested in it yet.
     */
    public boolean isBoundToOther(UUID playerId) {
        return owner.isPresent() && !isOwner(playerId);
    }

    public SpellBookData withOwner(UUID playerId) {
        return owner.isPresent() ? this : new SpellBookData(Optional.of(playerId), spellLevels, equippedPassive, equippedAggressive, investedPoints);
    }

    public SpellBookData withUnlocked(Spell spell) {
        Map<Identifier, Integer> newLevels = new HashMap<>(spellLevels);
        newLevels.put(spell.id(), 1);
        return new SpellBookData(owner, newLevels, equippedPassive, equippedAggressive, investedPoints + 1);
    }

    public SpellBookData withUpgraded(Spell spell) {
        Map<Identifier, Integer> newLevels = new HashMap<>(spellLevels);
        newLevels.put(spell.id(), levelOf(spell.id()) + 1);
        return new SpellBookData(owner, newLevels, equippedPassive, equippedAggressive, investedPoints + 1);
    }

    public SpellBookData withEquipped(Spell spell) {
        if (spell.type() == SpellType.PASSIVE) {
            return new SpellBookData(owner, spellLevels, Optional.of(spell.id()), equippedAggressive, investedPoints);
        }
        return new SpellBookData(owner, spellLevels, equippedPassive, Optional.of(spell.id()), investedPoints);
    }
}
