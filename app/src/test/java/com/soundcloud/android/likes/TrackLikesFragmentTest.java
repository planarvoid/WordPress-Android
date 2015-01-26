package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.android.Pager;

import android.view.View;
import android.widget.AdapterView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesFragmentTest {

    private TrackLikesFragment fragment;

    @Mock private LikeOperations likeOperations;
    @Mock private TrackLikesAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ShuffleViewController shuffleViewController;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private TrackLikesActionMenuController actionMenuController;
    @Mock private Subscription subscription;
    @Mock private Subscription shuffleSubscription;
    @Mock private Pager<List<PropertySet>> pager;

    @Before
    public void setUp() {
        Observable<List<PropertySet>> likedTracks = withSubscription(subscription, just(PropertySet.create())).toList();
        when(likeOperations.likedTracks()).thenReturn(likedTracks);
        when(likeOperations.likedTracksPager()).thenReturn(RxTestHelper.<List<PropertySet>>pagerWithSinglePage());
        when(listViewController.getEmptyView()).thenReturn(new EmptyView(Robolectric.application));

        Observable<List<Urn>> likedTrackUrns = withSubscription(shuffleSubscription, just(Urn.forTrack(123L))).toList();
        when(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrns);

        fragment = new TrackLikesFragment(adapter, likeOperations, listViewController,
                pullToRefreshController, shuffleViewController, playbackOperations,
                offlineOperations, actionMenuController,
                TestSubscribers.expandPlayerSubscriber());
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldUnsubscribeShuffleSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onViewCreated(mock(View.class), null);
        fragment.onDestroy();
        verify(shuffleSubscription).unsubscribe();
    }

    @Test
    public void shouldPlayLikedTracksOnListItemClick() {
        PropertySet clickedTrack = TestPropertySets.expectedLikedTrackForLikesScreen();
        AdapterView adapterView = setupAdapterView(clickedTrack);
        TestObservables.MockObservable<List<Urn>> playbackObservable = setupPlaybackOperations(clickedTrack);
        when(likeOperations.likedTracks()).thenReturn(Observable.<List<PropertySet>>empty());

        fragment.onItemClick(adapterView, mock(View.class), 0, 0);

        expect(playbackObservable.subscribedTo()).toBeTrue();
    }

    private AdapterView setupAdapterView(PropertySet clickedTrack) {
        AdapterView adapterView = mock(AdapterView.class);
        when(adapterView.getItemAtPosition(0)).thenReturn(clickedTrack);
        return adapterView;
    }

    private TestObservables.MockObservable<List<Urn>> setupPlaybackOperations(PropertySet clickedTrack) {
        TestObservables.MockObservable<List<Urn>> playbackObservable = TestObservables.emptyObservable();
        when(playbackOperations
                .playTracks(any(Observable.class), eq(clickedTrack.get(TrackProperty.URN)),
                        eq(0), eq(new PlaySessionSource(Screen.SIDE_MENU_LIKES))))
                .thenReturn(playbackObservable);
        return playbackObservable;
    }
}