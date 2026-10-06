package fail.holocraft.combat;

import fail.holocraft.sheet.IdolsRow;
import fail.holocraft.sheet.UpgradesRow;

/** An idol's weapon at a level: idols.json base row x the upgrades.json rows up to that level. */
public record WeaponStats(float damage, int cooldownTicks, float range, int hits, int pierce) {
    public static WeaponStats of(IdolsRow idol, int level, float haste) {
        float dmg = idol.damage(), cd = idol.cooldownTicks(), range = idol.range();
        int hits = 1, pierce = idol.pierce();
        for (UpgradesRow u : UpgradesRow.ALL) {
            if (u.level() != level) continue;
            dmg *= u.damageMult();
            cd *= u.cooldownMult();
            range *= u.rangeMult();
            hits += u.extraHits();
            pierce += u.extraPierce();
        }
        cd *= (1f - haste);
        return new WeaponStats(dmg, Math.max(4, Math.round(cd)), range, hits, pierce);
    }
}
