package com.cubiccadence.client.ui.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NowPlayingHudRendererTest {
    @Test
    void unsupportedCodePointsAreKeptWholeAndAssignedToFallbackRuns() {
        List<NowPlayingHudRenderer.LyricFontRun> runs = NowPlayingHudRenderer.splitLyricFontRuns(
                "AB你😀CD",
                codePoint -> codePoint < 0x80
        );

        assertEquals(List.of(
                new NowPlayingHudRenderer.LyricFontRun("AB", true),
                new NowPlayingHudRenderer.LyricFontRun("你😀", false),
                new NowPlayingHudRenderer.LyricFontRun("CD", true)
        ), runs);
    }

    @Test
    void emptyTextProducesNoFontRuns() {
        assertEquals(List.of(), NowPlayingHudRenderer.splitLyricFontRuns("", ignored -> true));
    }
}
