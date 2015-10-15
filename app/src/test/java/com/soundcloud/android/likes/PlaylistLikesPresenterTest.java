package com.soundcloud.android.likes;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemAdapter;
import com.soundcloud.android.playlists.PlaylistLikesPresenter;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.view.View;

import java.util.List;

public class PlaylistLikesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private PlaylistLikesPresenter presenter;

    @Mock private PlaylistLikeOperations likeOperations;
    @Mock private PlaylistItemAdapter adapter;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Navigator navigator;

    private TestEventBus testEventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        // TODO: Extract this common ListPresenter setup to a common base test class
        presenter = new PlaylistLikesPresenter(swipeRefreshAttacher, likeOperations,
                adapter, testEventBus, navigator);
        when(likeOperations.likedPlaylists()).thenReturn(Observable.<List<PropertySet>>empty());
        when(likeOperations.pagingFunction()).thenReturn(TestPager.<List<PropertySet>>singlePageFunction());
        when(likeOperations.onPlaylistLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onPlaylistUnliked()).thenReturn(Observable.<Urn>empty());
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() {
        PropertySet clickedPlaylist = TestPropertySets.expectedLikedPlaylistForPlaylistsScreen();
        when(adapter.getItem(0)).thenReturn(PlaylistItem.from(clickedPlaylist));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verify(navigator).openPlaylist(
                any(Context.class), eq(clickedPlaylist.get(PlaylistProperty.URN)), eq(Screen.SIDE_MENU_PLAYLISTS));
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        testEventBus.verifyUnsubscribed();
    }

}
