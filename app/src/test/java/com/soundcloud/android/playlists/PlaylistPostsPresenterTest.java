package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.view.View;

public class PlaylistPostsPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private PlaylistPostsPresenter presenter;

    private TestEventBus eventBus = new TestEventBus();
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private PlaylistItemAdapter adapter;
    @Mock private PlaylistPostOperations playlistPostOperations;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        presenter = new PlaylistPostsPresenter(adapter, playlistPostOperations, swipeRefreshAttacher, navigator, eventBus);
        when(playlistPostOperations.postedPlaylists()).thenReturn(just(singletonList(expectedPostedPlaylistsForPostedPlaylistsScreen())));
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() {
        PropertySet clickedPlaylist = expectedPostedPlaylistsForPostedPlaylistsScreen();
        when(adapter.getItem(0)).thenReturn(PlaylistItem.from(clickedPlaylist));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verify(navigator).openPlaylist(
                any(Context.class), eq(clickedPlaylist.get(PlaylistProperty.URN)), eq(Screen.SIDE_MENU_PLAYLISTS));
    }
}
