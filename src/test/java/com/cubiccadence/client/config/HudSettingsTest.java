package com.cubiccadence.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudSettingsTest {
    @Test
    void defaultsPreserveTheOriginalHudAppearance() {
        HudSettings settings = HudSettings.defaults();

        assertTrue(settings.enabled());
        assertTrue(settings.showCover());
        assertTrue(settings.showTitle());
        assertTrue(settings.showArtist());
        assertTrue(settings.showProgress());
        assertTrue(settings.showLyrics());
        assertFalse(settings.lyricsMode());
        assertTrue(settings.backgroundEnabled());
        assertEquals(1.0f, settings.scale());
        assertEquals(1.0f, settings.titleScale());
        assertEquals(1.0f, settings.lyricScale());
        assertEquals(HudSettings.DEFAULT_LYRIC_COLOR, settings.lyricColor());
        assertEquals(HudLyricFont.DEFAULT, settings.lyricFont());
        assertEquals(HudLyricWeight.REGULAR, settings.lyricWeight());
        assertEquals(HudPosition.TOP_LEFT, settings.position());
    }

    @Test
    void invalidValuesAreNormalizedAtTheSettingsBoundary() {
        HudSettings settings = new HudSettings(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                Float.NaN,
                8.0f,
                -3.0f,
                0x00123456,
                null,
                null,
                true,
                null,
                -500,
                500
        );

        assertEquals(1.0f, settings.scale());
        assertEquals(HudSettings.MAX_SCALE, settings.titleScale());
        assertEquals(HudSettings.MIN_SCALE, settings.lyricScale());
        assertEquals(0xFF123456, settings.lyricColor());
        assertEquals(HudLyricFont.DEFAULT, settings.lyricFont());
        assertEquals(HudLyricWeight.REGULAR, settings.lyricWeight());
        assertEquals(HudPosition.TOP_LEFT, settings.position());
        assertEquals(HudSettings.MIN_OFFSET, settings.offsetX());
        assertEquals(HudSettings.MAX_OFFSET, settings.offsetY());
    }

    @Test
    void unknownPersistedFontOptionsFallBackSafely() {
        assertEquals(HudLyricFont.DEFAULT, HudLyricFont.parse("REMOVED_FONT"));
        assertEquals(HudLyricWeight.REGULAR, HudLyricWeight.parse("HEAVY"));
    }

    @Test
    void anchorsUseMarginsAndCenterTheScaledHud() {
        assertEquals(8, HudPosition.TOP_LEFT.x(100, 20, 0, 8));
        assertEquals(40, HudPosition.TOP_CENTER.x(100, 20, 0, 8));
        assertEquals(72, HudPosition.TOP_RIGHT.x(100, 20, 0, 8));
        assertEquals(8, HudPosition.TOP_LEFT.y(60, 10, 0, 8));
        assertEquals(25, HudPosition.CENTER.y(60, 10, 0, 8));
        assertEquals(42, HudPosition.BOTTOM_RIGHT.y(60, 10, 0, 8));
    }

    @Test
    void offsetsCannotPushTheHudOutsideItsViewport() {
        assertEquals(0, HudPosition.TOP_LEFT.x(100, 20, -200, 8));
        assertEquals(80, HudPosition.BOTTOM_RIGHT.x(100, 20, 200, 8));
        assertEquals(0, HudPosition.TOP_LEFT.y(60, 10, -200, 8));
        assertEquals(50, HudPosition.BOTTOM_RIGHT.y(60, 10, 200, 8));
    }
}
