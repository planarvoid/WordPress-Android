package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class CastQueueSlicer {

    private static final int MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE = 100;
    private static final int HISTORY_BUFFER_SIZE = 10;

    @Inject
    CastQueueSlicer() {
    }

    PlayQueue slice(List<Urn> trackUrns, int pivotPosition) {
        return slice(trackUrns, pivotPosition, MAX_NUMBER_OF_TRACKS_CASTED_AT_ONCE, HISTORY_BUFFER_SIZE);
    }

    @VisibleForTesting
    PlayQueue slice(List<Urn> trackUrns, int pivotPosition, int windowSize, int historyBufferSize) {
        if (trackUrns.size() >= windowSize) {
            int from = Math.max(0, pivotPosition - historyBufferSize);

            if (from + windowSize > trackUrns.size()) {
                // Move the window backwards if pivot is close to the end
                from = trackUrns.size() - windowSize;
            }

            trackUrns = trackUrns.subList(from, from + windowSize);
        }
        return PlayQueue.fromTrackUrnList(trackUrns, PlaySessionSource.forCast(), Collections.emptyMap());
    }
}
