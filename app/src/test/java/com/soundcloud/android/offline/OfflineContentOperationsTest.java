package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeleteOfflineContentCommand;
import com.soundcloud.android.offline.commands.LoadPendingRemovalCommand;
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
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    @Mock private LoadPendingRemovalCommand loadPendingRemoval;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private UpdateOfflineContentCommand updateOfflineContent;
    @Mock private DeleteOfflineContentCommand deleteOfflineContent;
    @Mock private UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrns;
    @Mock private SecureFileStorage fileStorage;

    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);

    private OfflineContentOperations offlineOperations;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineSyncSetting;
    private TestScheduler delayScheduler;
    private TestSubscriber<Object> subscriber;

    @Before
    public void setUp() throws Exception {
        offlineSyncSetting = PublishSubject.create();
        eventBus = new TestEventBus();
        delayScheduler = new TestScheduler();
        offlineSyncSetting = PublishSubject.create();
        subscriber = new TestSubscriber<>();

        when(settingsStorage.isLikesOfflineSyncEnabled()).thenReturn(true);
        when(settingsStorage.getLikesOfflineSyncChanged()).thenReturn(offlineSyncSetting);

        when(loadLikedTrackUrns.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(loadPendingRemoval.toObservable()).thenReturn(Observable.<List<Urn>>empty());
        when(updateOfflineContent.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(updateContentAsPendingRemoval.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));

        offlineOperations = new OfflineContentOperations(
                updateOfflineContent,
                loadPendingRemoval,
                deleteOfflineContent,
                loadLikedTrackUrns,
                updateContentAsPendingRemoval,
                settingsStorage,
                eventBus,
                Schedulers.immediate(),
                delayScheduler
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
    public void startsOfflineSyncAfter3MinutesWhenATrackIsUnliked() {
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forTrack(123L), false, 1));

        expect(subscriber.getOnNextEvents()).toBeEmpty();
        delayScheduler.advanceTimeBy(3, TimeUnit.MINUTES);
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
        offlineOperations.startOfflineContentSyncing().subscribe(subscriber);

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

    @Test
    public void processPendingRemovalUpdatedAfterSuccessfulFileDeletion() throws Exception {
        final List<Urn> tracks = Arrays.asList(TRACK_URN);
        when(loadPendingRemoval.toObservable()).thenReturn(Observable.just(tracks));
        when(fileStorage.deleteTrack(TRACK_URN)).thenReturn(true);

        offlineOperations.processPendingRemovals().subscribe();

        expect(deleteOfflineContent.getInput()).toEqual(tracks);
    }
}
