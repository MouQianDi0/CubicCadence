package com.cubiccadence.model;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

/** Immutable synchronized lyrics shared by every music provider and HUD implementation. */
public record SyncedLyrics(
        String providerId,
        String trackId,
        List<LyricLine> lines
) {
    public SyncedLyrics {
        providerId = providerId == null ? "" : providerId.trim();
        trackId = trackId == null ? "" : trackId.trim();
        lines = lines == null
                ? List.of()
                : lines.stream()
                        .filter(java.util.Objects::nonNull)
                        .sorted(Comparator.comparingLong(LyricLine::startTimeMs))
                        .toList();
    }

    public Optional<LyricLine> currentLine(long positionMs) {
        int index = lineIndexAt(positionMs);
        return index < 0 ? Optional.empty() : Optional.of(lines.get(index));
    }

    public Optional<LyricLine> nextLine(long positionMs) {
        int index = lineIndexAt(positionMs);
        int nextIndex = index < 0 ? 0 : index + 1;
        return nextIndex >= lines.size() ? Optional.empty() : Optional.of(lines.get(nextIndex));
    }

    public int lineIndexAt(long positionMs) {
        int low = 0;
        int high = lines.size() - 1;
        int found = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (lines.get(middle).startTimeMs() <= positionMs) {
                found = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return found;
    }
}
