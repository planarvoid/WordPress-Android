package com.soundcloud.android.likes;

import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesFragmentTest {

    private TrackLikesFragment fragment;

    @Mock private LikeOperations likeOperations;
    @Mock private TrackLikesAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private ShuffleViewController shuffleViewController;
    @Mock private Subscription subscription;

    @Before
    public void setUp() {
        Observable<List<PropertySet>> likedTracks = withSubscription(subscription, just(PropertySet.create())).toList();
        when(likeOperations.likedTracks()).thenReturn(likedTracks);
        fragment = new TrackLikesFragment(adapter, likeOperations, listViewController,
                pullToRefreshController, shuffleViewController);
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }
}