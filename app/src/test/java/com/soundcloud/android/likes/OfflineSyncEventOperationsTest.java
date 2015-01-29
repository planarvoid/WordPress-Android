package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.offline.commands.OfflineTrackCountCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

@RunWith(SoundCloudTestRunner.class)
public class OfflineSyncEventOperationsTest {

    private static final int DOWNLOAD_LIKED_TRACKS_COUNT = 4;

    private OfflineSyncEventOperations operations;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private OfflineTrackCountCommand countDownloadedLikedTracks;

    @Before
    public void setUp() throws Exception {
        operations = new OfflineSyncEventOperations(countDownloadedLikedTracks, eventBus);
        when(countDownloadedLikedTracks.toObservable()).thenReturn(Observable.just(DOWNLOAD_LIKED_TRACKS_COUNT));
    }

    @Test
    public void callSyncFinishedOnOfflineIdleEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_SYNC, OfflineSyncEvent.idle());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncFinishedOnOfflineSyncStopEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_SYNC, OfflineSyncEvent.stop());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncStartedOnOfflineSyncStartEvent() {
        TestObserver<OfflineSyncEvent> observer = new TestObserver<>();
        OfflineSyncEvent event = OfflineSyncEvent.start();
        eventBus.publish(EventQueue.OFFLINE_SYNC, event);

        operations.onStarted().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(event);
    }

}