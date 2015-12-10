package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;

import java.util.List;

public class PlaybackUtils {

    public static int correctInitialPosition(PlayQueue playQueue, int startPosition, Urn initialTrack) {
        if (startPosition < playQueue.size() && playQueue.getUrn(startPosition).equals(initialTrack)) {
            return startPosition;
        } else {
            return playQueue.indexOfTrackUrn(initialTrack);
        }
    }

    @Deprecated
    // Prefer using PlayQueue rather than List<Urn> when possible
    public static int correctInitialPositionLegacy(List<Urn> tracks, int startPosition, Urn initialTrack) {
        if (startPosition < tracks.size() && tracks.get(startPosition).equals(initialTrack)) {
            return startPosition;
        } else {
            return tracks.indexOf(initialTrack);
        }
    }

    public static int correctStartPositionAndDeduplicateList(PlayQueue playQueue, int startPosition, Urn initialTrack, PlaySessionSource playSessionSource) {
        int updatedPosition = correctInitialPosition(playQueue, startPosition, initialTrack);

        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play an adapter track that's not in the list from " + playSessionSource));
            updatedPosition = 0;
        }

        return updatedPosition;
    }
}
