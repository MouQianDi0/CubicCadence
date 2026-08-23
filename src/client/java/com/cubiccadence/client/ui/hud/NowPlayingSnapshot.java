package com.cubiccadence.client.ui.hud;

import com.cubiccadence.model.PlaybackState;
import com.cubiccadence.model.LyricLine;
import com.cubiccadence.model.Track;

import java.util.List;

public record NowPlayingSnapshot(
        Track track,
        PlaybackState playbackState,
        long positionMs,
        long durationMs,
        List<LyricLine> lyricLines,
        int currentLyricIndex
) {
    public NowPlayingSnapshot {
        positionMs = Math.max(0L, positionMs);
        durationMs = Math.max(0L, durationMs);
        lyricLines = lyricLines == null ? List.of() : List.copyOf(lyricLines);
        if (currentLyricIndex < 0 || currentLyricIndex >= lyricLines.size()) {
            currentLyricIndex = -1;
        }
    }

    public String currentLyric() {
        return currentLyricIndex < 0 ? "" : lyricLines.get(currentLyricIndex).text();
    }

    public String nextLyric() {
        int nextIndex = currentLyricIndex < 0 ? 0 : currentLyricIndex + 1;
        return nextIndex >= lyricLines.size() ? "" : lyricLines.get(nextIndex).text();
    }
}
