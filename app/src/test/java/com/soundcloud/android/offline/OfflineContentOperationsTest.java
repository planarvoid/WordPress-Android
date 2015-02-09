package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.OfflineTrackCountCommand;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);
    private static final int DOWNLOAD_LIKED_TRACKS_COUNT = 4;

    @Mock private OfflineTrackCountCommand offlineTrackCount;
    @Mock private LoadPendingDownloadsCommand loadPendingDownloads;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private UpdateOfflineContentCommand updateOfflineContent;
    @Mock private DeletePendingRemovalCommand deleteOfflineContent;
    @Mock private UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrns;
    @Mock private SecureFileStorage fileStorage;
    @Mock private StoreCompletedDownloadCommand storeCompletedDownloadCommand;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineSyncSetting;
    private TestSubscriber<Object> subscriber;

    @Before
    public void setUp() throws Exception {
        offlineSyncSetting = PublishSubject.create();
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(settingsStorage.isOfflineLikesEnabled()).thenReturn(true);
        when(settingsStorage.getOfflineLikesChanged()).thenReturn(offlineSyncSetting);

        when(loadLikedTrackUrns.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(loadPendingDownloads.toObservable()).thenReturn(Observable.<List<DownloadRequest>>empty());
        when(updateOfflineContent.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(updateContentAsPendingRemoval.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(offlineTrackCount.toObservable()).thenReturn(Observable.just(DOWNLOAD_LIKED_TRACKS_COUNT));

        operations = new OfflineContentOperations(
                updateOfflineContent,
                loadLikedTrackUrns,
                loadPendingDownloads,
                updateContentAsPendingRemoval,
                settingsStorage,
                eventBus,
                offlineTrackCount);
    }

    @Test
    public void startOfflineSyncWhenFeatureIsEnabled() {
        operations.startOfflineContent().subscribe(subscriber);

        offlineSyncSetting.onNext(true);
        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isOfflineLikesEnabled()).thenReturn(false);
        operations.startOfflineContent().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(subscriber.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        operations.startOfflineContent().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(123L), true, 1));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        operations.startOfflineContent().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(123L), false, 1));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        operations.startOfflineContent().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        operations.startOfflineContent().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        expect(subscriber.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void updateOfflineLikesWhenOfflineLikesEnabled() throws PropellerWriteException {
        operations.updateDownloadRequestsFromLikes().subscribe(subscriber);

        offlineSyncSetting.onNext(true);

        expect(updateOfflineContent.getInput()).toContainExactly(TRACK_URN);
    }

    @Test
    public void updateOfflineLikesWhenOfflineLikesDisabled() throws PropellerWriteException {
        operations.stopOfflineContentService().subscribe(subscriber);

        offlineSyncSetting.onNext(false);

        verify(updateContentAsPendingRemoval).toObservable();
    }

    @Test
    public void updatesOfflineLikesWhenOfflineLikesDisabled() {
        operations.stopOfflineContentService().subscribe(subscriber);

        offlineSyncSetting.onNext(false);

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void callSyncFinishedOnOfflineIdleEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.idle());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncFinishedOnOfflineSyncStopEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.stop());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncStartedOnOfflineSyncStartEvent() {
        TestObserver<OfflineContentEvent> observer = new TestObserver<>();
        OfflineContentEvent event = OfflineContentEvent.start();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, event);

        operations.onStarted().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(event);
    }
}
