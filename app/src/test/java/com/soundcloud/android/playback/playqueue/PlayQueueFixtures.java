package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

class PlayQueueFixtures {

    static TrackPlayQueueUIItem getPlayQueueItem(int uniqueId) {
        final Urn track = Urn.forTrack(uniqueId);
        final PlaybackContext playbackContext = PlaybackContext.create(PlaySessionSource.EMPTY);
        final TrackQueueItem trackQueueItem = new TrackQueueItem(track, Urn.NOT_SET, Urn.NOT_SET, "source", "version",
                                                                 Optional.absent(), false, Urn.NOT_SET,
                                                                 Urn.NOT_SET, false, playbackContext, true);
        final TrackItem trackItem = PlayableFixtures.expectedTrackForListItem(track);
        final int someResourceId = 123;
        final int color = 321;
        return new TrackPlayQueueUIItem(trackQueueItem, trackItem, uniqueId, someResourceId, color, null,
                                        Optional.absent(), PlayQueueManager.RepeatMode.REPEAT_NONE);
    }

    static HeaderPlayQueueUIItem getHeaderPlayQueueUiItem() {
        final PlaybackContext playbackContext = PlaybackContext.create(PlaySessionSource.EMPTY);
        final Optional<String> title = Optional.of("Title");
        long id = 0;
        return new HeaderPlayQueueUIItem(
                PlayState.COMING_UP,
                PlayQueueManager.RepeatMode.REPEAT_NONE,
                true, id, "");
    }


}
