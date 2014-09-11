package com.soundcloud.android.explore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Provider;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksFragmentTest {

    private Bundle fragmentArgs = new Bundle();
    private ConnectableObservable<Page<SuggestedTracksCollection>> observable;

    private ExploreTracksFragment fragment;
    private FragmentActivity activity = new FragmentActivity();

    @Mock private PagingItemAdapter adapter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private ExploreTracksOperations exploreTracksOperations;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ListViewController listViewController;
    @Mock private Subscription subscription;
    @Mock private ExpandPlayerSubscriber subscriber;

    @Before
    public void setUp() throws Exception {
        fragment = new ExploreTracksFragment(adapter, playbackOperations, exploreTracksOperations,
                pullToRefreshController, listViewController, new Provider<ExpandPlayerSubscriber>() {
            @Override
            public ExpandPlayerSubscriber get() {
                return subscriber;
            }
        });
        fragmentArgs.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, ExploreGenre.POPULAR_AUDIO_CATEGORY);
        fragmentArgs.putString(ExploreTracksFragment.SCREEN_TAG_EXTRA, "screen");
        fragment.setArguments(fragmentArgs);
        Robolectric.shadowOf(fragment).setActivity(activity);

        observable = TestObservables.emptyConnectableObservable(subscription);
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(observable);
        when(playbackOperations.playTrackWithRecommendations(any(TrackUrn.class), any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<TrackUrn>>empty());
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragment.onCreate(null);
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldConnectListViewControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(listViewController).connect(refEq(fragment), any(ConnectableObservable.class));
    }

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
        final ApiTrack track = TestHelper.getModelFactory().createModel(ApiTrack.class);
        when(adapter.getItem(0)).thenReturn(track);

        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        verify(playbackOperations).playTrackWithRecommendations(new PublicApiTrack(track).getUrn(), playSessionSource);
    }

    @Test
    public void shouldPassAlongTrackingTagWhenPlayingTrack() throws CreateModelException {
        SuggestedTracksCollection collection = new SuggestedTracksCollection();
        collection.setTrackingTag("tag");
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class)))
                .thenReturn(Observable.just(RxTestHelper.singlePage(collection)));
        final ApiTrack track = TestHelper.getModelFactory().createModel(ApiTrack.class);
        when(adapter.getItem(0)).thenReturn(track);

        fragment.onCreate(null);
        fragment.onItemClick(null, null, 0, 0);

        final PlaySessionSource playSessionSource = new PlaySessionSource("screen");
        playSessionSource.setExploreVersion("tag");
        verify(playbackOperations).playTrackWithRecommendations(new PublicApiTrack(track).getUrn(), playSessionSource);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(activity.getLayoutInflater(), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
