package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PagedPlaylistsAdapter;
import com.soundcloud.android.playlists.PlaylistDetailFragment;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistLikesPresenter;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistLikesPresenterTest {

    private PlaylistLikesPresenter presenter;

    @Mock private PlaylistLikeOperations likeOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private PagedPlaylistsAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;

    @Captor private ArgumentCaptor<AdapterView.OnItemClickListener> onItemClickCaptor;

    private TestEventBus testEventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        // TODO: Extract this common ListPresenter setup to a common base test class
        presenter = new PlaylistLikesPresenter(imageOperations, pullToRefreshWrapper, likeOperations, adapter, testEventBus);
        when(fragmentView.findViewById(android.R.id.list)).thenReturn(listView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(likeOperations.likedPlaylists()).thenReturn(Observable.<List<PropertySet>>empty());
        when(likeOperations.likedPlaylistsPager()).thenReturn(RxTestHelper.<List<PropertySet>>pagerWithSinglePage());
        when(likeOperations.onPlaylistLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onPlaylistUnliked()).thenReturn(Observable.<Urn>empty());
        when(listView.getContext()).thenReturn(Robolectric.application);
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PropertySet clickedPlaylist = TestPropertySets.expectedLikedPlaylistForPlaylistsScreen();
        when(adapter.getItem(0)).thenReturn(PlaylistItem.from(clickedPlaylist));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        getOnItemClickListener().onItemClick(listView, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailFragment.EXTRA_URN))
                .toEqual(clickedPlaylist.get(PlaylistProperty.URN));
        expect(Screen.fromIntent(intent)).toBe(Screen.SIDE_MENU_PLAYLISTS);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);
        presenter.onDestroyView(fragment);

        testEventBus.verifyUnsubscribed();
    }

    private AdapterView.OnItemClickListener getOnItemClickListener() {
        verify(listView).setOnItemClickListener(onItemClickCaptor.capture());
        return onItemClickCaptor.getValue();
    }
}
