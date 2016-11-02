package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TrackLikesTrackItemTest extends AndroidUnitTest {
    private static final Urn PLAYED_TRACK_URN = Urn.forTrack(123L);
    private static final Urn CURRENT_TRACK_URN = Urn.forTrack(124L);
    @Mock private TrackItem trackItem;
    @Mock private CurrentPlayQueueItemEvent event;
    @Mock private PlayableQueueItem queueItem;

    private TrackLikesTrackItem trackLikesTrackItem;

    @Before
    public void setUp() throws Exception {
        trackLikesTrackItem = new TrackLikesTrackItem(trackItem);
        when(event.getCurrentPlayQueueItem()).thenReturn(queueItem);
        when(queueItem.getUrnOrNotSet()).thenReturn(PLAYED_TRACK_URN);
    }

    @Test
    public void setsNowPlayingWhenUrnMatchesAndNotCurrentlyPlaying() throws Exception {
        when(trackItem.getUrn()).thenReturn(PLAYED_TRACK_URN);
        when(trackItem.isPlaying()).thenReturn(false);

        final boolean updated = trackLikesTrackItem.updateNowPlaying(event);

        verify(trackItem).setIsPlaying(true);
        assertThat(updated).isTrue();
    }

    @Test
    public void removesNowPlayingWhenUrnDoesNotMatchAndPlaying() throws Exception {
        when(trackItem.getUrn()).thenReturn(CURRENT_TRACK_URN);
        when(trackItem.isPlaying()).thenReturn(true);

        final boolean updated = trackLikesTrackItem.updateNowPlaying(event);

        verify(trackItem).setIsPlaying(false);
        assertThat(updated).isTrue();
    }

    @Test
    public void doesNotSetNowPlayingWhenDifferentUrnAndNotPlaying() throws Exception {
        when(trackItem.getUrn()).thenReturn(CURRENT_TRACK_URN);
        when(trackItem.isPlaying()).thenReturn(false);

        final boolean updated = trackLikesTrackItem.updateNowPlaying(event);

        verify(trackItem, never()).setIsPlaying(anyBoolean());
        assertThat(updated).isFalse();
    }
}
