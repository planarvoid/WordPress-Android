package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.TrackLikeOperations.INITIAL_TIMESTAMP;
import static com.soundcloud.android.likes.TrackLikeOperations.PAGE_SIZE;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.likes.LoadLikedTracksCommand.Params;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Func2;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackLikeOperationsTest extends AndroidUnitTest {

    private TrackLikeOperations operations;

    @Mock private Observer<List<LikeWithTrack>> observer;
    @Mock private TrackItemRepository trackRepository;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private ConnectionHelper connectionHelper;
    @Mock private Action0 requestSystemSyncAction;
    @Captor private ArgumentCaptor<Func2<TrackItem, Like, LikeWithTrack>> functionCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private List<TrackItem> tracks;
    private List<Like> likes;

    private PublishSubject<Void> syncSubject = PublishSubject.create();
    private List<LikeWithTrack> likeWithTracks;

    @Before
    public void setUp() throws Exception {
        operations = new TrackLikeOperations(
                loadLikedTracksCommand,
                syncInitiator,
                syncInitiatorBridge,
                eventBus,
                scheduler,
                connectionHelper,
                trackRepository
        );
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(syncSubject);

        tracks = ModelFixtures.trackItems(2);

        likes = asList(
                Like.create(tracks.get(0).getUrn(), new Date(100)),
                Like.create(tracks.get(1).getUrn(), new Date(100)));

        likeWithTracks = asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1)));

        when(loadLikedTracksCommand.toObservable(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE)))).thenReturn(
                Observable.just(likes));
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));
    }

    @Test
    public void syncAndLoadTrackLikesWhenHasNotSyncedBefore() {
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(false));
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Observable.just(urnToTrackMap(tracks)));

        operations.likedTracks().subscribe(observer);

        verify(observer, never()).onNext(anyList());

        syncSubject.onNext(null);
        syncSubject.onCompleted();

        verify(observer).onNext(likeWithTracks);
        verify(observer).onCompleted();
    }

    @Test
    public void loadTrackLikesWhenHasSyncedBefore() {
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Observable.just(urnToTrackMap(tracks)));

        operations.likedTracks().subscribe(observer);

        assertThat(syncSubject.hasObservers()).isFalse();

        verify(observer).onNext(asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1))));
        verify(observer).onCompleted();
    }

    @Test
    public void loadEmptyTrackLikesWhenHasSyncedBefore() {
        when(loadLikedTracksCommand.toObservable(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Observable.just(Collections.emptyList()));

        when(trackRepository.fromUrns(emptyList()))
                .thenReturn(Observable.just(Collections.emptyMap()));


        operations.likedTracks().subscribe(observer);

        assertThat(syncSubject.hasObservers()).isFalse();

        verify(observer).onNext(emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void loadTrackLikesRequestsUpdatesFromSyncerOnWifi() {
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Observable.just(urnToTrackMap(tracks)));

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator).batchSyncTracks(asList(likes.get(0).urn(), likes.get(1).urn()));
    }

    @Test
    public void loadTrackLikesDoesNotRequestUpdatesFromSyncerOffWifi() {
        when(connectionHelper.isWifiConnected()).thenReturn(false);
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Observable.just(urnToTrackMap(tracks)));

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).batchSyncTracks(anyList());
    }

    @Test
    public void loadEmptyTrackLikesDoesNotRequestUpdatesFromSyncer() {
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(loadLikedTracksCommand.toObservable(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Observable.just(Collections.emptyList()));

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).batchSyncTracks(anyList());
    }

    @Test
    public void updatedLikedTracksReloadsLikedTracksAfterSyncWithChange() {
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Observable.just(urnToTrackMap(tracks)));

        operations.updatedLikedTracks().subscribe(observer);

        verify(observer, never()).onNext(anyList());

        syncSubject.onNext(null);
        syncSubject.onCompleted();

        verify(observer).onNext(asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1))));
        verify(observer).onCompleted();
    }

    @Test
    public void updatedLikedTracksReloadsEmptyLikedTracksAfterSyncWithChange() {
        when(loadLikedTracksCommand.toObservable(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Observable.just(Collections.emptyList()));

        when(trackRepository.fromUrns(emptyList()))
                .thenReturn(Observable.just(Collections.emptyMap()));

        operations.updatedLikedTracks().subscribe(observer);

        verify(observer, never()).onNext(anyList());

        syncSubject.onNext(null);
        syncSubject.onCompleted();

        verify(observer).onNext(emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void onTrackLikedEventReturnsTrackInfoFromLike() throws Exception {
        TrackItem track = ModelFixtures.expectedLikedTrackForLikesScreen();
        when(trackRepository.track(track.getUrn())).thenReturn(Observable.just(track));

        final TestSubscriber<TrackItem> observer = new TestSubscriber<>();
        operations.onTrackLiked().subscribe(observer);
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(track.getUrn(), true, 5));

        assertThat(observer.getOnNextEvents()).containsExactly(track);
    }

    @Test
    public void onTrackUnlikedEventReturnsUnlikedTrackUrn() throws Exception {
        final Urn unlikedTrackUrn = Urn.forTrack(123L);
        final TestObserver<Urn> observer = new TestObserver<>();
        operations.onTrackUnliked().subscribe(observer);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(unlikedTrackUrn, false, 5));

        assertThat(observer.getOnNextEvents()).containsExactly(unlikedTrackUrn);
    }

    private Map<Urn, TrackItem> urnToTrackMap(List<TrackItem> tracks) {
        Map<Urn, TrackItem> map = new HashMap<>(tracks.size());
        for (TrackItem trackItem : tracks) {
            map.put(trackItem.getUrn(), trackItem);
        }
        return map;
    }
}
