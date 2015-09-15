package com.soundcloud.android.likes;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistLikesAdapter;
import com.soundcloud.android.playlists.PlaylistLikesPresenter;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public class PlaylistLikesPresenterTest extends AndroidUnitTest {

    private PlaylistLikesPresenter presenter;

    @Mock private PlaylistLikeOperations likeOperations;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private PlaylistLikesAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private Activity context;
    @Mock private View fragmentView;
    @Mock private RecyclerView recyclerView;
    @Mock private View itemView;
    @Mock private EmptyView emptyView;
    @Mock private Navigator navigator;

    private TestEventBus testEventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        // TODO: Extract this common ListPresenter setup to a common base test class
        presenter = new PlaylistLikesPresenter(swipeRefreshAttacher, likeOperations,
                adapter, testEventBus, navigator);
        when(fragmentView.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(likeOperations.likedPlaylists()).thenReturn(Observable.<List<PropertySet>>empty());
        when(likeOperations.pagingFunction()).thenReturn(TestPager.<List<PropertySet>>singlePageFunction());
        when(likeOperations.onPlaylistLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onPlaylistUnliked()).thenReturn(Observable.<Urn>empty());

        when(fragmentView.getContext()).thenReturn(context());
        when(fragmentView.getResources()).thenReturn(resources());
        when(itemView.getContext()).thenReturn(context());
        when(itemView.getResources()).thenReturn(resources());
        when(recyclerView.getAdapter()).thenReturn(adapter);
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() {
        PropertySet clickedPlaylist = TestPropertySets.expectedLikedPlaylistForPlaylistsScreen();
        when(adapter.getItem(0)).thenReturn(PlaylistItem.from(clickedPlaylist));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).openPlaylist(context(), clickedPlaylist.get(PlaylistProperty.URN), Screen.SIDE_MENU_PLAYLISTS);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
        presenter.onDestroyView(fragment);

        testEventBus.verifyUnsubscribed();
    }
}
