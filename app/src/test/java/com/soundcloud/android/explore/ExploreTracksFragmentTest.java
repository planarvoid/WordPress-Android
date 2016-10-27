package com.soundcloud.android.explore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startVisibleFragment;
import static rx.Observable.just;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.ListViewController;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExploreTracksFragmentTest extends AndroidUnitTest {

    private Bundle fragmentArgs = new Bundle();

    private ExploreTracksFragment fragment;

    @Mock private PagingListItemAdapter<TrackItem> adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ImageOperations imageOperations;
    @Mock private ExploreTracksOperations exploreTracksOperations;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ListViewController listViewController;

    @Before
    public void setUp() throws Exception {
        when(exploreTracksOperations.pager()).thenReturn(TestPager.<SuggestedTracksCollection>pagerWithSinglePage());
        Observable<SuggestedTracksCollection> observable = Observable.just(new SuggestedTracksCollection(Collections.<ApiTrack>emptyList(),
                                                                                                         null,
                                                                                                         null,
                                                                                                         "1.0"));
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(observable);
        when(playbackInitiator.playTracks(any(List.class), anyInt(), any(PlaySessionSource.class)))
                .thenReturn(Observable.<PlaybackResult>empty());
        fragment = new ExploreTracksFragment(adapter,
                                             playbackInitiator,
                                             exploreTracksOperations,
                                             pullToRefreshController,
                                             listViewController,
                                             TestSubscribers.expandPlayerSubscriber());
        fragmentArgs.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, ExploreGenre.POPULAR_AUDIO_CATEGORY);
        fragmentArgs.putString(ExploreTracksFragment.SCREEN_TAG_EXTRA, "screen");
        fragment.setArguments(fragmentArgs);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        startVisibleFragment(fragment);

        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldRefreshTracksByRecreatingObservableThatLoadsFirstPage() {
        startVisibleFragment(fragment);

        ConnectableObservable<List<TrackItem>> listConnectableObservable = fragment.refreshObservable();
        listConnectableObservable.connect();

        TestSubscriber<List<TrackItem>> testSubscriber = new TestSubscriber<>();
        listConnectableObservable.subscribe(testSubscriber);
        verify(exploreTracksOperations, times(2)).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter, times(1)).onCompleted();
        testSubscriber.assertCompleted();
    }

    @Test
    public void shouldConnectListViewControllerInOnViewCreated() {
        startVisibleFragment(fragment);

        verify(listViewController).connect(refEq(fragment), any(ConnectableObservable.class));
    }

    @Test
    public void shouldConnectPullToRefreshControllerInOnViewCreated() {
        startVisibleFragment(fragment);

        verify(pullToRefreshController).connect(any(ConnectableObservable.class), same(adapter));
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        final PublishSubject<SuggestedTracksCollection> suggestedTracks = PublishSubject.create();
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(suggestedTracks);
        startVisibleFragment(fragment);
        fragment.onDestroy();

        assertThat(suggestedTracks.hasObservers()).isFalse();
    }

    @Test
    public void shouldPlaySelectedTrackWhenItemClicked() throws CreateModelException {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(adapter.getItems()).thenReturn(Arrays.asList(TrackItem.from(track)));

        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        verify(playbackInitiator).playTracks(Arrays.asList(track.getUrn()), 0, playSessionSource);
    }

    @Test
    public void shouldPassAlongTrackingTagWhenPlayingTrack() throws CreateModelException {
        SuggestedTracksCollection collection = new SuggestedTracksCollection(null, null, null, "1.0");
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class)))
                .thenReturn(just(collection));
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(adapter.getItems()).thenReturn(Arrays.asList(TrackItem.from(track)));

        fragment.onCreate(null);
        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = PlaySessionSource.forExplore("screen", "1.0");
        verify(playbackInitiator).playTracks(Arrays.asList(track.getUrn()), 0, playSessionSource);
    }

}
