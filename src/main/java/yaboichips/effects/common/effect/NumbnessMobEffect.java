package yaboichips.effects.common.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * While active, incoming damage is fully absorbed instead of applied (see the
 * {@code ALLOW_DAMAGE} hook in {@code Effects}, which banks it here). Once the
 * effect ends, {@code NumbnessEffectMixin} pays out {@link #PAYOUT_FRACTION}
 * of whatever was banked as real damage.
 */
public class NumbnessMobEffect extends MobEffect {

    public static final float PAYOUT_FRACTION = 0.25f;

    private static final Map<UUID, Float> absorbedDamage = new HashMap<>();

    public NumbnessMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x6E7B8B);
    }

    public static void bank(UUID playerId, float amount) {
        absorbedDamage.merge(playerId, amount, Float::sum);
    }

    public static float takePayout(UUID playerId) {
        Float total = absorbedDamage.remove(playerId);
        return total == null ? 0.0f : total * PAYOUT_FRACTION;
    }
}
