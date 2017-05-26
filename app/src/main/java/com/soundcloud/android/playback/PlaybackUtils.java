package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;

public class PlaybackUtils {

    public static int correctStartPosition(PlayQueue playQueue,
                                           int startPosition,
                                           Urn initialTrack,
                                           PlaySessionSource playSessionSource) {
        int updatedPosition = getPositionInList(playQueue, startPosition, initialTrack);
        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException(
                    "Attempting to play an adapter track that's not in the list from " + playSessionSource));
            updatedPosition = 0;
        }
        return updatedPosition;
    }

    private static int getPositionInList(PlayQueue playQueue, int initialPosition, Urn initialTrack) {
        int correctedPosition = 0;

        if (playQueue.hasItems() && initialPosition < playQueue.size()) {
            if (initialPosition >= 0 && playQueue.getUrn(initialPosition).equals(initialTrack)) {
                correctedPosition = initialPosition;
            } else {
                correctedPosition = playQueue.indexOfTrackUrn(initialTrack);
            }
        }

        return correctedPosition;
    }
}
