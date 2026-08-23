package com.cubiccadence.client.config;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** Font families available to both compact and lyrics-only HUD rendering. */
public enum HudLyricFont {
    DEFAULT(FontDescription.DEFAULT),
    UNIFORM(new FontDescription.Resource(Identifier.withDefaultNamespace("uniform"))),
    CUSTOM(new FontDescription.Resource(Identifier.fromNamespaceAndPath("cubic-cadence", "custom")));

    private final FontDescription description;

    HudLyricFont(FontDescription description) {
        this.description = description;
    }

    public FontDescription description() {
        return description;
    }

    public static HudLyricFont parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return DEFAULT;
        }
    }
}
