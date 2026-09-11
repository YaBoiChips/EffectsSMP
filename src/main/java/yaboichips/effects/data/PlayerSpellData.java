package yaboichips.effects.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Player-level spell point currency, earned via scrolls/amulets. Spell
 * unlocks/levels/equips themselves live on the bound {@link SpellBookData}
 * component instead, so points spent through a book stay invested in that
 * specific book rather than following the player.
 */
public record PlayerSpellData(int spellPoints) {

    public static final PlayerSpellData DEFAULT = new PlayerSpellData(3);

    public static final Codec<PlayerSpellData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("spell_points").forGetter(PlayerSpellData::spellPoints)
    ).apply(instance, PlayerSpellData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PlayerSpellData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public PlayerSpellData withSpellPoints(int newSpellPoints) {
        return new PlayerSpellData(newSpellPoints);
    }
}
