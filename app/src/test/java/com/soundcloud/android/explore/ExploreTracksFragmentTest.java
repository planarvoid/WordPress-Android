package com.soundcloud.android.explore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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
    private ConnectableObservable<Page<SuggestedTracksCollection>> observable;

    @InjectMocks
    private ExploreTracksFragment fragment;
    private FragmentActivity activity = new FragmentActivity();

    @Mock private PagingItemAdapter adapter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private ExploreTracksOperations exploreTracksOperations;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ListViewController listViewController;
    @Mock private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        fragmentArgs.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, ExploreGenre.POPULAR_AUDIO_CATEGORY);
        fragmentArgs.putString(ExploreTracksFragment.SCREEN_TAG_EXTRA, "screen");
        fragment.setArguments(fragmentArgs);
        Robolectric.shadowOf(fragment).setActivity(activity);

        observable = TestObservables.emptyConnectableObservable(subscription);
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(observable);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragment.onCreate(null);
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldAttachListViewControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(listViewController).onViewCreated(refEq(fragment), any(ConnectableObservable.class),
                refEq(fragment.getView()), refEq(adapter), isA(AbsListViewParallaxer.class));
    }

    @Test
    public void shouldAttachPullToRefreshControllerInOnViewCreated() {
        fragment.onCreate(null);
        fragment.connectObservable(observable);
        createFragmentView();
        verify(pullToRefreshController).onViewCreated(fragment, observable, adapter);
    }

    @Test
    public void shouldDetachPullToRefreshControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(pullToRefreshController).onDestroyView();
    }

    @Test
    public void shouldDetachListViewControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(listViewController).onDestroyView();
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
