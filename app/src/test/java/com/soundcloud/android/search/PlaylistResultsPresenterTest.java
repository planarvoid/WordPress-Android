package com.soundcloud.android.search;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.view.View;

public class PlaylistResultsPresenterTest extends AndroidUnitTest {

    private static final String SEARCH_TAG = "tag";

    @Rule public final FragmentRule fragmentRule = new FragmentRule(
            R.layout.default_recyclerview_with_refresh, new Bundle());
    @Mock private PlaylistDiscoveryOperations operations;
    @Mock private PlaylistResultsAdapter adapter;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Navigator navigator;
    @Mock private View view;

    private TestEventBus eventBus = new TestEventBus();
    private ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);

    private PlaylistResultsPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(view.getContext()).thenReturn(fragmentRule.getActivity());
        fakePlaylistResultsForTag(SEARCH_TAG);
        presenter = new PlaylistResultsPresenter(operations, adapter, swipeRefreshAttacher, navigator, eventBus);
    }

    @Test
    public void shouldPerformPlaylistTagSearchWithTagFromBundleInOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(PlaylistItem.from(playlist)));
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() {
        final PlaylistItem clickedPlaylist = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onItemClicked(view, 0);

        verify(navigator).legacyOpenPlaylist(fragmentRule.getActivity(),
                                             clickedPlaylist.getUrn(),
                                             Screen.SEARCH_PLAYLIST_DISCO);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistTagResultsIsClicked() {
        when(adapter.getItem(0)).thenReturn(ModelFixtures.create(PlaylistItem.class));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onItemClicked(view, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind().isPresent()).isFalse();
    }

    @Test
    public void shouldNotifyAdapterWhenPlaylistEntityStateChanged() {
        when(adapter.getItems()).thenReturn(Lists.newArrayList(PlaylistItem.from(playlist)));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, fakeLikePlaylistEvent(playlist.getUrn()));

        verify(adapter).notifyItemChanged(0);
    }

    private EntityStateChangedEvent fakeLikePlaylistEvent(Urn playlistUrn) {
        return EntityStateChangedEvent.fromLike(playlistUrn, true, playlist.getLikesCount() + 1);
    }

    private void fakePlaylistResultsForTag(String searchTag) {
        ApiPlaylistCollection collection = new ApiPlaylistCollection(singletonList(playlist), null, null);
        SearchResult searchResult = SearchResult.fromPropertySetSource(collection.getCollection(),
                                                                       Optional.<Link>absent(), Optional.<Urn>absent());
        when(operations.playlistsForTag(searchTag)).thenReturn(Observable.just(searchResult));

        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString(PlaylistResultsFragment.KEY_PLAYLIST_TAG, searchTag);
        fragmentRule.setFragmentArguments(fragmentArgs);
    }

}
