package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
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
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    @Mock private LoadPendingDownloadsCommand loadPendingDownloads;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private UpdateOfflineContentCommand updateOfflineContent;
    @Mock private DeletePendingRemovalCommand deleteOfflineContent;
    @Mock private UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrns;
    @Mock private SecureFileStorage fileStorage;
    @Mock private StoreCompletedDownloadCommand storeCompletedDownloadCommand;

    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);

    private OfflineContentOperations offlineOperations;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineSyncSetting;
    private TestSubscriber<Object> subscriber;

    @Before
    public void setUp() throws Exception {
        offlineSyncSetting = PublishSubject.create();
        eventBus = new TestEventBus();
        offlineSyncSetting = PublishSubject.create();
        subscriber = new TestSubscriber<>();

        when(settingsStorage.isLikesOfflineSyncEnabled()).thenReturn(true);
        when(settingsStorage.getLikesOfflineSyncChanged()).thenReturn(offlineSyncSetting);

        when(loadLikedTrackUrns.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(loadPendingDownloads.toObservable()).thenReturn(Observable.<List<DownloadRequest>>empty());
        when(updateOfflineContent.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(updateContentAsPendingRemoval.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));

        offlineOperations = new OfflineContentOperations(
                updateOfflineContent,
                loadLikedTrackUrns,
                loadPendingDownloads,
                updateContentAsPendingRemoval,
                settingsStorage,
                eventBus
        );
    }

    @Test
    public void startOfflineSyncWhenFeatureIsEnabled() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        offlineSyncSetting.onNext(true);
        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isLikesOfflineSyncEnabled()).thenReturn(false);
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(subscriber.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(123L), true, 1));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(123L), false, 1));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        expect(subscriber.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void updateOfflineLikesWhenOfflineLikesEnabled() throws PropellerWriteException {
        offlineOperations.updateDownloadRequestsFromLikes().subscribe(subscriber);

        offlineSyncSetting.onNext(true);

        expect(updateOfflineContent.getInput()).toContainExactly(TRACK_URN);
    }

    @Test
    public void updateOfflineLikesWhenOfflineLikesDisabled() throws PropellerWriteException {
        offlineOperations.stopOfflineContentSyncing().subscribe(subscriber);

        offlineSyncSetting.onNext(false);

        verify(updateContentAsPendingRemoval).toObservable();
    }

    @Test
    public void updatesOfflineLikesWhenOfflineLikesDisabled() {
        offlineOperations.stopOfflineContentSyncing().subscribe(subscriber);

        offlineSyncSetting.onNext(false);

        expect(subscriber.getOnNextEvents()).toNumber(1);
    }
}
