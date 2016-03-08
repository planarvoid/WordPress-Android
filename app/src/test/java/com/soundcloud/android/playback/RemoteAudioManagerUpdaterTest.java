package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DisplayMetricsStub;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

public class RemoteAudioManagerUpdaterTest extends AndroidUnitTest {

    @Mock private IRemoteAudioManager audioManager;
    @Mock private TrackRepository trackRepository;
    @Mock private Resources resources;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private ImageOperations imageOperations;
    @Captor private ArgumentCaptor<Pair<PropertySet,Optional<Bitmap>>> pairCaptor;

    private Bitmap bitmap;
    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;
    private PropertySet track;
    private TestEventBus eventBus = new TestEventBus();
    private DisplayMetrics displayMetrics = new DisplayMetricsStub();

    private RemoteAudioManagerUpdater updater;

    @Before
    public void setUp() throws Exception {
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        updater = new RemoteAudioManagerUpdater(trackRepository, InjectionSupport.lazyOf(audioManager), eventBus,
                playQueueManager, imageOperations, resources);
        updater.subscribe();

        track = expectedTrackForPlayer().put(AdProperty.IS_AUDIO_AD, false);
        trackUrn = track.get(TrackProperty.URN);
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

        when(trackRepository.track(trackUrn)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(true);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    }

    @Test
    public void playQueueTrackChangedHandlerDoesNotSetTrackOnAudioManagerIfTrackChangeNotSupported() {
        when(audioManager.isTrackChangeSupported()).thenReturn(false);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(audioManager, never()).onTrackChanged(any(PropertySet.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentAudioAdTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));


        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn, AdFixtures.getAudioAd(Urn.forTrack(123L)));
        track.put(AdProperty.IS_AUDIO_AD, true);
        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));

        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(eq(track), any(Bitmap.class));
    }

    @Test
    public void playQueueTrackChangedHandlerSetsLockScreenStateWithNullBitmapForCurrentTrackOnImageLoadError() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.<Bitmap>error(new Exception("Could not load image")));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        verify(audioManager).onTrackChanged(track, null);
    }

    @Test
    public void playQueueChangedHandlerDoesntSetLockScreenStateForCurrentVideoAd() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        final VideoQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(trackUrn));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(videoItem, Urn.NOT_SET, 0));
        verify(audioManager, never()).onTrackChanged(any(PropertySet.class), any(Bitmap.class));
    }

    @Test
    public void playQueueChangedHandlerSetsLockScreenStateWithBitmapForCurrentTrack() {
        when(audioManager.isTrackChangeSupported()).thenReturn(true);
        when(imageOperations.artwork(trackUrn, ApiImageSize.T500)).thenReturn(Observable.just(bitmap));

        InOrder inOrder = Mockito.inOrder(audioManager);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(trackPlayQueueItem, Urn.NOT_SET, 0));
        inOrder.verify(audioManager).onTrackChanged(track, null);
        inOrder.verify(audioManager).onTrackChanged(eq(track), any(Bitmap.class));
    }
}
