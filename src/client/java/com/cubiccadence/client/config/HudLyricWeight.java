package com.cubiccadence.client.config;

/** Reliable font-weight choices supported by Minecraft's text renderer. */
public enum HudLyricWeight {
    REGULAR(false),
    BOLD(true);

    private final boolean bold;

    HudLyricWeight(boolean bold) {
        this.bold = bold;
    }

    public boolean bold() {
        return bold;
    }

    public static HudLyricWeight parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return REGULAR;
        }
    }
}
