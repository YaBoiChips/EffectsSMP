package yaboichips.effects.client.core;

/**
 * Local client-side "deafened" window, refreshed by
 * {@link yaboichips.effects.network.SetDeafenedPayload} and consulted by the
 * client-only sound-suppression mixin to approximate Stealth's Smokescreen
 * "deafness" without touching the player's actual volume settings.
 * Self-expiring: the server just keeps refreshing this while a player is
 * inside the smoke zone, so it naturally decays a couple of seconds after the
 * last refresh with no explicit "un-deafen" message needed.
 */
public final class DeafenState {

    private static volatile long deafenedUntilMillis;

    private DeafenState() {
    }

    public static boolean isDeafened() {
        return System.currentTimeMillis() < deafenedUntilMillis;
    }

    public static void refresh(int durationTicks) {
        deafenedUntilMillis = System.currentTimeMillis() + durationTicks * 50L;
    }
}
