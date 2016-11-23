package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.TrackLikeOperations.INITIAL_TIMESTAMP;
import static com.soundcloud.android.likes.TrackLikeOperations.PAGE_SIZE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackLikeOperationsTest extends AndroidUnitTest {

    private TrackLikeOperations operations;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private LikedTrackStorage likedTrackStorage;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new TrackLikeOperations(
                loadLikedTrackUrnsCommand,
                likedTrackStorage,
                syncInitiator,
                syncInitiatorBridge,
                eventBus,
                scheduler,
                networkConnectionHelper);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void syncAndLoadTrackLikesWhenInitialTrackLoadReturnsEmptyList() {
        List<PropertySet> likedTracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE,
                                              INITIAL_TIMESTAMP)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()),
                                                                             Observable.just(likedTracks));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>just(null));

        operations.likedTracks().subscribe(observer);

        verify(observer).onNext(likedTracks);
        verify(observer).onCompleted();
    }

    @Test
    public void likedTracksReturnsLikedTracksFromStorage() {
        List<PropertySet> likedTracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP)).thenReturn(Observable.just(likedTracks));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>empty());

        operations.likedTracks().subscribe(observer);

        verify(observer).onNext(likedTracks);
        verify(observer).onCompleted();
    }

    @Test
    public void likedTracksRequestsUpdatesFromSyncer() {
        List<PropertySet> likedTracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP)).thenReturn(Observable.just(likedTracks));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator).batchSyncTracks(PropertySets.extractUrns(likedTracks));
    }

    @Test
    public void likedTracksDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        List<PropertySet> likedTracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP)).thenReturn(Observable.just(likedTracks));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>empty());

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).batchSyncTracks(any(ArrayList.class));
    }

    @Test
    public void likedTracksRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE,
                                              INITIAL_TIMESTAMP)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>empty());

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).batchSyncTracks(any(ArrayList.class));
    }

    @Test
    public void updatedLikedTracksReloadsLikedTracksAfterSyncWithChange() {
        List<PropertySet> likedTracks = singletonList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP)).thenReturn(Observable.just(likedTracks));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>just(null));

        operations.updatedLikedTracks().subscribe(observer);

        InOrder inOrder = Mockito.inOrder(observer, syncInitiatorBridge);
        inOrder.verify(syncInitiatorBridge).syncTrackLikes();
        inOrder.verify(observer).onNext(likedTracks);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void syncAndLoadEmptyTrackLikesResults() {
        when(likedTrackStorage.loadTrackLikes(PAGE_SIZE, INITIAL_TIMESTAMP))
                .thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiatorBridge.syncTrackLikes()).thenReturn(Observable.<Void>just(null));

        operations.likedTracks().subscribe(observer);

        verify(observer).onNext(Collections.<PropertySet>emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void onTrackLikedEventReturnsTrackInfoFromLike() throws Exception {
        final PropertySet likedTrack = TestPropertySets.expectedLikedTrackForLikesScreen();
        when(likedTrackStorage.loadTrackLike(likedTrack.get(TrackProperty.URN))).thenReturn(Observable.just(likedTrack));

        final TestObserver<PropertySet> observer = new TestObserver<>();
        operations.onTrackLiked().subscribe(observer);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(likedTrack.get(TrackProperty.URN), true, 5));

        assertThat(observer.getOnNextEvents()).containsExactly(likedTrack);
    }

    @Test
    public void onTrackUnlikedEventReturnsUnlikedTrackUrn() throws Exception {
        final Urn unlikedTrackUrn = Urn.forTrack(123L);
        final TestObserver<Urn> observer = new TestObserver<>();
        operations.onTrackUnliked().subscribe(observer);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(unlikedTrackUrn, false, 5));

        assertThat(observer.getOnNextEvents()).containsExactly(unlikedTrackUrn);
    }
}
