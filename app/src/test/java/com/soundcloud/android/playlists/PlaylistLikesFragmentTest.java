package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.Actions;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.app.Application;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistLikesFragmentTest {

    private PlaylistLikesFragment fragment;

    @Mock private LikeOperations likeOperations;
    @Mock private PlaylistLikesAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private Subscription subscription;

    private AdapterView adapterView;
    private Application context;

    @Before
    public void setUp() throws Exception {
        Observable<List<PropertySet>> likedPlaylists = withSubscription(subscription, just(PropertySet.create())).toList();
        when(likeOperations.likedPlaylists()).thenReturn(likedPlaylists);
        context = Robolectric.application;
        when(listViewController.getEmptyView()).thenReturn(new EmptyView(context));
        fragment = new PlaylistLikesFragment(adapter, likeOperations, listViewController, pullToRefreshController);
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PropertySet clickedPlaylist = TestPropertySets.expectedLikedPlaylistForPlaylistsScreen();
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);
        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(adapterView, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailFragment.EXTRA_URN))
                .toEqual(clickedPlaylist.get(PlaylistProperty.URN));
        expect(Screen.fromIntent(intent)).toBe(Screen.SIDE_MENU_PLAYLISTS);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        fragment.onViewCreated(layout, null);
        adapterView = (AdapterView) fragment.getView().findViewById(android.R.id.list);
        return layout;
    }
}