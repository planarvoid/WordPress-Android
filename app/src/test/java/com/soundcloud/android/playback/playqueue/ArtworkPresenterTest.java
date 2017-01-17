package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.PLAYBACK_PROGRESS;
import static com.soundcloud.android.events.EventQueue.PLAYBACK_STATE_CHANGED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

public class ArtworkPresenterTest extends AndroidUnitTest {

    private final TrackItem trackItem = ModelFixtures.trackItem();
    private final PlayQueueItem playQueueItem = new TrackQueueItem.Builder(Urn.forTrack(1L))
            .withPlaybackContext(PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT))
            .build();

    private final TestEventBus eventBus = new TestEventBus();

    @Mock private ArtworkView artworkView;
    @Mock private TrackRepository trackRepository;

    private ArtworkPresenter artworkPresenter;

    @Before
    public void setUp() {
        when(trackRepository.track(any())).thenReturn(Observable.just(trackItem));

        artworkPresenter = new ArtworkPresenter(eventBus, trackRepository);
        artworkPresenter.attachView(artworkView);
    }

    @Test
    public void doNotCallContractWhenNotAttached() {
        artworkPresenter.detachView();
        eventBus.publish(PLAYBACK_PROGRESS, PlaybackProgressEvent.create(PlaybackProgress.empty(), Urn.NOT_SET));
        eventBus.publish(PLAYBACK_STATE_CHANGED, PlayStateEvent.create(PlaybackStateTransition.DEFAULT, 0L, true, "playId"));
        eventBus.publish(CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(PlayQueueItem.EMPTY, Urn.NOT_SET, 0));

        verifyZeroInteractions(artworkView);
    }

    @Test
    public void callSetImageOnNewQueueEvent() {
        PlayQueueItem playQueueItem = new TrackQueueItem.Builder(Urn.forTrack(1L))
                .withPlaybackContext(PlaybackContext.create(PlaybackContext.Bucket.EXPLICIT))
                .build();

        eventBus.publish(CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.forPlaylist(2L), 0));

        verify(artworkView, times(1)).setImage(SimpleImageResource.create(trackItem.getUrn(), trackItem.getImageUrlTemplate()));
    }

    @Test
    public void callSetPlaybackProgress() {
        PlaybackProgress playbackProgress = new PlaybackProgress(50, 100, trackItem.getUrn());

        eventBus.publish(CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.forPlaylist(2L), 0));
        eventBus.publish(PLAYBACK_PROGRESS, PlaybackProgressEvent.create(playbackProgress, trackItem.getUrn()));


        verify(artworkView, times(1)).setPlaybackProgress(playbackProgress, 100);
    }

    @Test
    public void callStartProgressAnimations() {
        PlayStateEvent playStateEvent = PlayStateEvent.create(new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, trackItem.getUrn(), 50L, 100L), 100L, true, "playId");

        eventBus.publish(CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(playQueueItem, Urn.forPlaylist(2L), 0));
        eventBus.publish(PLAYBACK_STATE_CHANGED, playStateEvent);

        verify(artworkView).startProgressAnimation(playStateEvent.getProgress(), 100L);
    }

    @Test
    public void calculateStartXandStartY() {
        artworkPresenter.artworkSizeChanged(100, 1000);

        verify(artworkView).setProgressControllerValues(0, -900);
    }

    @Test
    public void doNotCalculateCallSetProgressControllerValues() {
        artworkPresenter.artworkSizeChanged(0, 100);
        artworkPresenter.artworkSizeChanged(100, 0);
        artworkPresenter.artworkSizeChanged(0, 0);

        verify(artworkView, never()).setProgressControllerValues(anyInt(), anyInt());
    }

}
