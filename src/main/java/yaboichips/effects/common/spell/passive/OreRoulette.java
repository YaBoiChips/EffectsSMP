package yaboichips.effects.common.spell.passive;

import net.minecraft.util.RandomSource;

/**
 * The weighted ore tiers Fortune Level 2's on-hit roulette can land on, low
 * (common, weak) to high (rare, strong) - coal through diamond as specified.
 */
public enum OreRoulette {
    COAL("coal", 1.0f, 40),
    IRON("iron", 2.0f, 25),
    GOLD("gold", 3.0f, 15),
    REDSTONE("redstone", 4.0f, 10),
    EMERALD("emerald", 6.0f, 7),
    DIAMOND("diamond", 8.0f, 3);

    private final String key;
    private final float damage;
    private final int weight;

    OreRoulette(String key, float damage, int weight) {
        this.key = key;
        this.damage = damage;
        this.weight = weight;
    }

    public String key() {
        return key;
    }

    public float damage() {
        return damage;
    }

    public static OreRoulette roll(RandomSource random) {
        int totalWeight = 0;
        for (OreRoulette tier : values()) {
            totalWeight += tier.weight;
        }
        int roll = random.nextInt(totalWeight);
        for (OreRoulette tier : values()) {
            roll -= tier.weight;
            if (roll < 0) {
                return tier;
            }
        }
        return DIAMOND;
    }
}
