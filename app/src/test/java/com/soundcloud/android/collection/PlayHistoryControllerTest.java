package com.soundcloud.android.collection;


import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class PlayHistoryControllerTest extends AndroidUnitTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(234L);
    private static final Urn COLLECTION_URN = Urn.forArtistStation(987L);

    @Mock WritePlayHistoryCommand storeCommand;

    private Scheduler scheduler = Schedulers.immediate();
    private EventBus eventBus = new TestEventBus();
    private TestDateProvider dateProvider = new TestDateProvider();

    @Before
    public void setUp() throws Exception {
        dateProvider.setTime(0, TimeUnit.MILLISECONDS);
        PlayHistoryController controller = new PlayHistoryController(eventBus, dateProvider, storeCommand, scheduler);
        controller.subscribe();
        publishPlayQueue(COLLECTION_URN);
    }

    @Test
    public void storesLongPlays() {
        publishProgressAndAdvanceTime(TRACK_URN, 0, 0);
        publishProgressAndAdvanceTime(TRACK_URN, 10000, 10000);

        verify(storeCommand, times(1)).call(PlayHistoryRecord.create(0, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void doesNotStoreShortPlays() {
        publishProgressAndAdvanceTime(TRACK_URN, 0, 0);
        publishProgressAndAdvanceTime(TRACK_URN, 5000, 5000);

        verify(storeCommand, never()).call(PlayHistoryRecord.create(0, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void doesNotStorePlaysThatStartAfterThreshold() {
        publishProgressAndAdvanceTime(TRACK_URN, 1000, 0);
        publishProgressAndAdvanceTime(TRACK_URN, 11000, 10000);

        verify(storeCommand, never()).call(PlayHistoryRecord.create(0, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void scrubDoesNotStorePlayWhenLessThanThreshold() {
        publishProgressAndAdvanceTime(TRACK_URN, 0, 0);
        publishProgressAndAdvanceTime(TRACK_URN, 25000, 5000);

        verify(storeCommand, never()).call(PlayHistoryRecord.create(0, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void resetsOnNewTrack() {
        publishProgressAndAdvanceTime(TRACK_URN, 0, 0);
        publishProgressAndAdvanceTime(TRACK_URN, 7000, 7000);
        publishProgressAndAdvanceTime(TRACK_URN2, 0, 5000);
        publishProgressAndAdvanceTime(TRACK_URN2, 10000, 10000);

        verify(storeCommand, times(1)).call(PlayHistoryRecord.create(12000, TRACK_URN2, COLLECTION_URN));
    }

    private void publishPlayQueue(Urn collectionUrn) {
        PlayQueueEvent event = new PlayQueueEvent(PlayQueueEvent.NEW_QUEUE, collectionUrn);
        eventBus.publish(EventQueue.PLAY_QUEUE, event);
    }

    private void publishProgress(Urn trackUrn, long position) {
        PlaybackProgress playbackProgress = new PlaybackProgress(position, 1000L);
        PlaybackProgressEvent event = PlaybackProgressEvent.create(playbackProgress, trackUrn);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);
    }

    private void publishProgressAndAdvanceTime(Urn trackUrn, long progressPosition, long timeElapsed) {
        dateProvider.advanceBy(timeElapsed, TimeUnit.MILLISECONDS);
        publishProgress(trackUrn, progressPosition);
    }


}
