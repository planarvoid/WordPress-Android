package com.soundcloud.android.explore;

import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksFragmentTest {

    private Bundle fragmentArgs = new Bundle();

    private ExploreTracksFragment fragment;
    private FragmentActivity activity = new FragmentActivity();

    @Mock private PagingListItemAdapter<TrackItem> adapter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private ExploreTracksOperations exploreTracksOperations;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ListViewController listViewController;
    @Mock private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        Observable<SuggestedTracksCollection> observable = withSubscription(subscription, just(new SuggestedTracksCollection()));
        when(exploreTracksOperations.pager()).thenReturn(TestPager.<SuggestedTracksCollection>pagerWithSinglePage());
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(observable);
        when(playbackOperations.playTrackWithRecommendationsLegacy(any(Urn.class), any(PlaySessionSource.class)))
                .thenReturn(Observable.<PlaybackResult>empty());
        fragment = new ExploreTracksFragment(adapter, playbackOperations, exploreTracksOperations,
                pullToRefreshController, listViewController, TestSubscribers.expandPlayerSubscriber());
        fragmentArgs.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, ExploreGenre.POPULAR_AUDIO_CATEGORY);
        fragmentArgs.putString(ExploreTracksFragment.SCREEN_TAG_EXTRA, "screen");
        fragment.setArguments(fragmentArgs);
        Robolectric.shadowOf(fragment).setActivity(activity);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragment.onCreate(null);
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldRefreshTracksByRecreatingObservableThatLoadsFirstPage() {
        fragment.refreshObservable().connect();
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Ignore // RL1 doesn't support dealing with resources from AARs
    @Test
    public void shouldConnectListViewControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(listViewController).connect(refEq(fragment), any(ConnectableObservable.class));
    }

    @Ignore // RL1 doesn't support dealing with resources from AARs
    @Test
    public void shouldConnectPullToRefreshControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(pullToRefreshController).connect(any(ConnectableObservable.class), same(adapter));
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldPlaySelectedTrackWhenItemClicked() throws CreateModelException {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(adapter.getItem(0)).thenReturn(TrackItem.from(track));

        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        verify(playbackOperations).playTrackWithRecommendationsLegacy(track.getUrn(), playSessionSource);
    }

    @Test
    public void shouldPassAlongTrackingTagWhenPlayingTrack() throws CreateModelException {
        SuggestedTracksCollection collection = new SuggestedTracksCollection();
        collection.setTrackingTag("tag");
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class)))
                .thenReturn(just(collection));
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        when(adapter.getItem(0)).thenReturn(TrackItem.from(track));

        fragment.onCreate(null);
        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setExploreVersion("tag");
        verify(playbackOperations).playTrackWithRecommendationsLegacy(track.getUrn(), playSessionSource);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(activity.getLayoutInflater(), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
