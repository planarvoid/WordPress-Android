package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.withSubscription;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.Actions;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.android.LegacyPager;

import android.app.Application;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.List;

@Ignore // RL1 doesn't support dealing with resources from AARs
@RunWith(SoundCloudTestRunner.class)
public class PlaylistPostsFragmentTest {

    private PlaylistPostsFragment fragment;

    @Mock private PlaylistPostOperations playlistPostOperations;
    @Mock private PlaylistPostsAdapter adapter;
    @Mock private ListViewController listViewController;
    @Mock private PullToRefreshController pullToRefreshController;
    @Mock private Subscription subscription;
    @Mock private LegacyPager<List<PropertySet>> pager;

    private AdapterView adapterView;
    private Application context;

    @Before
    public void setUp() throws Exception {
        Observable<List<PropertySet>> postedPlaylists = withSubscription(subscription, just(PropertySet.create())).toList();
        when(playlistPostOperations.postedPlaylists()).thenReturn(postedPlaylists);
        when(playlistPostOperations.postedPlaylistsPager()).thenReturn(TestPager.<List<PropertySet>>pagerWithSinglePage());
        context = Robolectric.application;
        when(listViewController.getEmptyView()).thenReturn(new EmptyView(context));
        fragment = new PlaylistPostsFragment(adapter, playlistPostOperations, listViewController, pullToRefreshController);
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PlaylistItem clickedPlaylist = PlaylistItem.from(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);
        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(adapterView, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailFragment.EXTRA_URN))
                .toEqual(clickedPlaylist.getEntityUrn());
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
