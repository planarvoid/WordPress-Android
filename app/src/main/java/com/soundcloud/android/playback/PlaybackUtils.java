package com.soundcloud.android.playback;

import com.google.common.collect.Sets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlaybackUtils {

    public static int correctInitialPosition(List<Urn> trackUrns, int startPosition, Urn initialTrack) {
        if (startPosition < trackUrns.size() && trackUrns.get(startPosition).equals(initialTrack)) {
            return startPosition;
        } else {
            return trackUrns.indexOf(initialTrack);
        }
    }

    public static int correctStartPositionAndDeduplicateList(List<Urn> trackUrns, int startPosition, Urn initialTrack) {
        int updatedPosition = correctInitialPosition(trackUrns, startPosition, initialTrack);

        if (updatedPosition < 0) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to play an adapter track that's not in the list"));
            updatedPosition = 0;
        }

        return getDeduplicatedList(trackUrns, updatedPosition);
    }

    /**
     * Remove duplicates from playqueue, preserving the ordering with regards to the item they clicked on
     * Returns the new startPosition
     */
    // TODO: This method should return de-duplicated list, instead of mutating the original one
    private static int getDeduplicatedList(List<Urn> trackUrns, int startPosition) {
        final Set<Urn> seenTracks = Sets.newHashSetWithExpectedSize(trackUrns.size());
        final Urn playedTrack = trackUrns.get(startPosition);

        int i = 0;
        Iterator<Urn> iterator = trackUrns.iterator();
        int adjustedPosition = startPosition;
        while (iterator.hasNext()) {
            final Urn track = iterator.next();
            if (i != adjustedPosition && (seenTracks.contains(track) || track.equals(playedTrack))) {
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
