package yaboichips.effects.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

public record TrustData(Set<UUID> trustedPlayers) {

    public static final TrustData DEFAULT = new TrustData(Set.of());

    public static final Codec<TrustData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC_SET.fieldOf("trusted_players").forGetter(TrustData::trustedPlayers)
    ).apply(instance, TrustData::new));

    public boolean isTrusted(UUID playerId) {
        return trustedPlayers.contains(playerId);
    }

    public TrustData withTrusted(UUID playerId) {
        if (isTrusted(playerId)) {
            return this;
        }
        Set<UUID> updated = new HashSet<>(trustedPlayers);
        updated.add(playerId);
        return new TrustData(updated);
    }

    public TrustData withUntrusted(UUID playerId) {
        if (!isTrusted(playerId)) {
            return this;
        }
        Set<UUID> updated = new HashSet<>(trustedPlayers);
        updated.remove(playerId);
        return new TrustData(updated);
    }
}
