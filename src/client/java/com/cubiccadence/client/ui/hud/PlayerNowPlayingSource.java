package com.cubiccadence.client.ui.hud;

import com.cubiccadence.client.lyrics.LyricsManager;
import com.cubiccadence.client.playback.PlayerController;
import com.cubiccadence.model.PlaybackState;
import com.cubiccadence.model.SyncedLyrics;
import com.cubiccadence.model.Track;

import java.util.Objects;
import java.util.Optional;

/** Adapts the current player and lyric manager to the HUD's provider-neutral contract. */
public final class PlayerNowPlayingSource implements NowPlayingSource {
    private final PlayerController playerController;
    private final LyricsManager lyricsManager;

    public PlayerNowPlayingSource(PlayerController playerController, LyricsManager lyricsManager) {
        this.playerController = Objects.requireNonNull(playerController, "playerController");
        this.lyricsManager = Objects.requireNonNull(lyricsManager, "lyricsManager");
    }

    @Override
    public Optional<NowPlayingSnapshot> snapshot() {
        Track track = playerController.getCurrentTrack();
        PlaybackState state = playerController.getState();
        if (track == null || state == PlaybackState.IDLE || state == PlaybackState.ENDED) {
            return Optional.empty();
        }
        long timelinePosition = playerController.getTimelinePositionMs();
        SyncedLyrics lyrics = lyricsManager.getLyrics()
                .filter(value -> track.providerId().equals(value.providerId())
                        && track.trackId().equals(value.trackId()))
                .orElse(null);
        return Optional.of(new NowPlayingSnapshot(
                track,
                state,
                playerController.getPositionMs(),
                playerController.getDurationMs(),
                lyrics == null ? java.util.List.of() : lyrics.lines(),
                lyrics == null ? -1 : lyrics.lineIndexAt(timelinePosition)
        ));
    }
}
