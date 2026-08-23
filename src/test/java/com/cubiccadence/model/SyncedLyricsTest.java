package com.cubiccadence.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncedLyricsTest {
    private final SyncedLyrics lyrics = new SyncedLyrics("netease", "track", List.of(
            new LyricLine(1_000L, "first", "第一句"),
            new LyricLine(2_000L, "second", "第二句"),
            new LyricLine(3_000L, "third", "第三句")
    ));

    @Test
    void lineIndexTracksBeforeExactAndBetweenTimestamps() {
        assertEquals(-1, lyrics.lineIndexAt(999L));
        assertEquals(0, lyrics.lineIndexAt(1_000L));
        assertEquals(0, lyrics.lineIndexAt(1_999L));
        assertEquals(1, lyrics.lineIndexAt(2_000L));
        assertEquals(2, lyrics.lineIndexAt(Long.MAX_VALUE));
    }
}
