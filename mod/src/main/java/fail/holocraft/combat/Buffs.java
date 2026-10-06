package fail.holocraft.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived special effects per player (game-time deadlines). Not saved: a special ends with the session. */
public final class Buffs {
    public static final class State {
        public long damageUntil, slowUntil, speedUntil, reviveUntil, spinUntil, cooldownHalfUntil;
        public float damageMult = 1f;
        public float spinDamage;
        public long nextAttack;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static State of(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    public static void clear(UUID id) { STATES.remove(id); }

    private Buffs() {}
}
