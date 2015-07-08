package com.soundcloud.android.playback;

import com.google.common.collect.Sets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlaybackUtils {

    public static int correctInitialPosition(PlayQueue playQueue, int startPosition, Urn initialTrack) {
        if (startPosition < playQueue.size() && playQueue.getUrn(startPosition).equals(initialTrack)) {
            return startPosition;
        } else {
            return playQueue.indexOf(initialTrack);
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

    public static int correctStartPositionAndDeduplicateList(PlayQueue playQueue, int startPosition, Urn initialTrack) {
        int updatedPosition = correctInitialPosition(playQueue, startPosition, initialTrack);

        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play an adapter track that's not in the list"));
            updatedPosition = 0;
        }

        return getDeduplicatedList(playQueue, updatedPosition);
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    // TODO: This method should return de-duplicated list, instead of mutating the original one
    private static int getDeduplicatedList(PlayQueue trackUrns, int startPosition) {
        final Set<PlayQueueItem> seenTracks = Sets.newHashSetWithExpectedSize(trackUrns.size());
        final Urn playedTrack = trackUrns.getUrn(startPosition);

        int i = 0;
        Iterator<PlayQueueItem> iterator = trackUrns.iterator();
        int adjustedPosition = startPosition;
        while (iterator.hasNext()) {
            final PlayQueueItem track = iterator.next();
            if (i != adjustedPosition && (seenTracks.contains(track) || track.getTrackUrn().equals(playedTrack))) {
                iterator.remove();
                if (i < adjustedPosition) {
                    adjustedPosition--;
                }
            } else {
                seenTracks.add(track);
                i++;
            }
        }
        return adjustedPosition;
    }
}
