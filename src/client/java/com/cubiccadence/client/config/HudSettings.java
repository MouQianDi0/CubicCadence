package com.cubiccadence.client.config;

/** Immutable HUD options shared by the settings preview and the in-game renderer. */
public record HudSettings(
        boolean enabled,
        boolean showCover,
        boolean showTitle,
        boolean showArtist,
        boolean showProgress,
        boolean showLyrics,
        boolean lyricsMode,
        float scale,
        float titleScale,
        float lyricScale,
        int lyricColor,
        HudLyricFont lyricFont,
        HudLyricWeight lyricWeight,
        boolean backgroundEnabled,
        HudPosition position,
        int offsetX,
        int offsetY
) {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;
    public static final int MIN_OFFSET = -200;
    public static final int MAX_OFFSET = 200;
    public static final int DEFAULT_LYRIC_COLOR = 0xFF9ED7A8;

    public HudSettings {
        scale = clampScale(scale);
        titleScale = clampScale(titleScale);
        lyricScale = clampScale(lyricScale);
        lyricColor = 0xFF000000 | (lyricColor & 0x00FFFFFF);
        lyricFont = lyricFont == null ? HudLyricFont.DEFAULT : lyricFont;
        lyricWeight = lyricWeight == null ? HudLyricWeight.REGULAR : lyricWeight;
        position = position == null ? HudPosition.TOP_LEFT : position;
        offsetX = clampOffset(offsetX);
        offsetY = clampOffset(offsetY);
    }

    public static HudSettings defaults() {
        return new HudSettings(
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                1.0f,
                1.0f,
                1.0f,
                DEFAULT_LYRIC_COLOR,
                HudLyricFont.DEFAULT,
                HudLyricWeight.REGULAR,
                true,
                HudPosition.TOP_LEFT,
                0,
                0
        );
    }

    public int lyricRed() {
        return lyricColor >> 16 & 0xFF;
    }

    public int lyricGreen() {
        return lyricColor >> 8 & 0xFF;
    }

    public int lyricBlue() {
        return lyricColor & 0xFF;
    }

    private static float clampScale(float value) {
        if (!Float.isFinite(value)) {
            return 1.0f;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    private static int clampOffset(int value) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
    }
}
