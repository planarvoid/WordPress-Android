package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.ListViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksFragmentTest {

    private Bundle fragmentArgs = new Bundle();
    private TestObservables.MockObservable observable;

    @InjectMocks
    private ExploreTracksFragment fragment;

    @Mock
    private FragmentActivity activity;
    @Mock
    private ExploreTracksAdapter adapter;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private ExploreTracksOperations exploreTracksOperations;
    @Mock
    private PullToRefreshController pullToRefreshController;
    @Mock
    private ListViewController listViewController;
    @Mock
    private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        fragmentArgs.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, ExploreGenre.POPULAR_AUDIO_CATEGORY);
        fragment.setArguments(fragmentArgs);
        Robolectric.shadowOf(fragment).setActivity(activity);

        observable = TestObservables.emptyObservable(subscription);
        when(exploreTracksOperations.getSuggestedTracks(any(ExploreGenre.class))).thenReturn(observable);
    }

    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWithGenreFromBundleInOnCreate() {
        fragment.onCreate(null);
        expect(observable.subscribedTo()).toBeTrue();
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldRestartObservableWithRefreshSubscriberWhenRefreshing() {
        fragment.onRefreshStarted(null);
        expect(observable.subscribedTo()).toBeTrue();
        verify(exploreTracksOperations).getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY);
        verify(adapter).onCompleted();
    }

    @Test
    public void shouldDetachPullToRefreshControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(pullToRefreshController).detach();
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
}
