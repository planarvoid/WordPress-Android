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
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.functions.Func2;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackLikeOperationsTest extends AndroidUnitTest {

    private TrackLikeOperations operations;

    @Mock private TrackItemRepository trackRepository;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private ConnectionHelper connectionHelper;
    @Captor private ArgumentCaptor<Func2<TrackItem, Like, LikeWithTrack>> functionCaptor;

    private TestEventBusV2 eventBus = new TestEventBusV2();
    private Scheduler scheduler = Schedulers.trampoline();
    private List<TrackItem> tracks;
    private List<Like> likes;

    private SingleSubject<SyncJobResult> syncSubject = SingleSubject.create();
    private List<LikeWithTrack> likeWithTracks;

    @Before
    public void setUp() throws Exception {
        operations = new TrackLikeOperations(
                loadLikedTracksCommand,
                syncInitiatorBridge,
                eventBus,
                scheduler,
                trackRepository
        );
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(syncSubject);

        tracks = ModelFixtures.trackItems(2);

        likes = asList(
                Like.create(tracks.get(0).getUrn(), new Date(100)),
                Like.create(tracks.get(1).getUrn(), new Date(100)));

        likeWithTracks = asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1)));

        when(loadLikedTracksCommand.toSingle(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE)))).thenReturn(
                Single.just(likes));
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));
    }

    @Test
    public void syncAndLoadTrackLikesWhenHasNotSyncedBefore() {
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(false));
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Single.just(urnToTrackMap(tracks)));

        final TestObserver<List<LikeWithTrack>> observer = operations.likedTracks().test();

        observer.assertNoValues();

        syncSubject.onSuccess(TestSyncJobResults.successWithoutChange());

        observer.assertValue(likeWithTracks);
    }

    @Test
    public void loadTrackLikesWhenHasSyncedBefore() {
        when(syncInitiatorBridge.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Single.just(urnToTrackMap(tracks)));

        final TestObserver<List<LikeWithTrack>> observer = operations.likedTracks().test();

        assertThat(syncSubject.hasObservers()).isFalse();

        observer.assertValue(asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1))));
    }

    @Test
    public void loadEmptyTrackLikesWhenHasSyncedBefore() {
        when(loadLikedTracksCommand.toSingle(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Single.just(Collections.emptyList()));

        when(trackRepository.fromUrns(emptyList()))
                .thenReturn(Single.just(Collections.emptyMap()));


        final TestObserver<List<LikeWithTrack>> observer = operations.likedTracks().test();

        assertThat(syncSubject.hasObservers()).isFalse();

        observer.assertValue(emptyList());
    }

    @Test
    public void loadTrackLikesDoesNotRequestUpdatesFromSyncerOffWifi() {
        when(connectionHelper.isWifiConnected()).thenReturn(false);
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Single.just(urnToTrackMap(tracks)));

        operations.likedTracks().test();

        verify(syncInitiator, never()).batchSyncTracks(anyList());
    }

    @Test
    public void loadEmptyTrackLikesDoesNotRequestUpdatesFromSyncer() {
        when(connectionHelper.isWifiConnected()).thenReturn(true);
        when(loadLikedTracksCommand.toSingle(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Single.just(Collections.emptyList()));

        operations.likedTracks().test();

        verify(syncInitiator, never()).batchSyncTracks(anyList());
    }

    @Test
    public void updatedLikedTracksReloadsLikedTracksAfterSyncWithChange() {
        when(trackRepository.fromUrns(eq(transform(asList(likes.get(0), likes.get(1)), UrnHolder::urn))))
                .thenReturn(Single.just(urnToTrackMap(tracks)));

        final TestObserver<List<LikeWithTrack>> observer = operations.updatedLikedTracks().test();

        observer.assertNoValues();

        syncSubject.onSuccess(TestSyncJobResults.successWithoutChange());

        observer.assertValue(asList(
                LikeWithTrack.create(likes.get(0), tracks.get(0)),
                LikeWithTrack.create(likes.get(1), tracks.get(1))));
    }

    @Test
    public void updatedLikedTracksReloadsEmptyLikedTracksAfterSyncWithChange() {
        when(loadLikedTracksCommand.toSingle(Optional.of(Params.from(INITIAL_TIMESTAMP, PAGE_SIZE))))
                .thenReturn(Single.just(Collections.emptyList()));

        when(trackRepository.fromUrns(emptyList()))
                .thenReturn(Single.just(Collections.emptyMap()));

        final TestObserver<List<LikeWithTrack>> observer = operations.updatedLikedTracks().test();

        observer.assertNoValues();

        syncSubject.onSuccess(TestSyncJobResults.successWithoutChange());

        observer.assertValue(emptyList());
    }

    @Test
    public void onTrackLikedEventReturnsTrackInfoFromLike() throws Exception {
        TrackItem track = ModelFixtures.expectedLikedTrackForLikesScreen();
        when(trackRepository.track(track.getUrn())).thenReturn(Maybe.just(track));

        final TestObserver<TrackItem> observer = operations.onTrackLiked().test();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(track.getUrn(), true, 5));

        assertThat(observer.values()).containsExactly(track);
    }

    @Test
    public void onTrackUnlikedEventReturnsUnlikedTrackUrn() throws Exception {
        final Urn unlikedTrackUrn = Urn.forTrack(123L);
        final TestObserver<Urn> observer = new TestObserver<>();
        operations.onTrackUnliked().subscribe(observer);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(unlikedTrackUrn, false, 5));

        assertThat(observer.values()).containsExactly(unlikedTrackUrn);
    }

    private Map<Urn, TrackItem> urnToTrackMap(List<TrackItem> tracks) {
        Map<Urn, TrackItem> map = new HashMap<>(tracks.size());
        for (TrackItem trackItem : tracks) {
            map.put(trackItem.getUrn(), trackItem);
        }
        return map;
    }
}
