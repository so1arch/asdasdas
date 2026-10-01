package com.medievalcombat.combat;

/** Five attack / guard zones, KCD-style. The id is the ordinal. */
public enum Zone {
    UP(1.15f),     // overhead
    LEFT(1.0f),
    RIGHT(1.0f),
    DOWN(0.9f),    // low strike
    THRUST(1.0f);  // centre: thrust (damage taken from the weapon's own thrust value)

    public final float slashMult;

    Zone(float slashMult) {
        this.slashMult = slashMult;
    }

    public static Zone byId(int id) {
        Zone[] v = values();
        return id >= 0 && id < v.length ? v[id] : THRUST;
    }
}
