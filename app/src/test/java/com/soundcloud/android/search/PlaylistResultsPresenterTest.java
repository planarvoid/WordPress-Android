package com.soundcloud.android.search;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
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

    private void fakePlaylistResultsForTag(String searchTag) {
        ApiPlaylistCollection collection = new ApiPlaylistCollection();
        collection.setCollection(singletonList(playlist));
        when(operations.playlistsForTag(searchTag)).thenReturn(Observable.just(collection));

        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString(PlaylistResultsFragment.KEY_PLAYLIST_TAG, searchTag);
        fragmentRule.setFragmentArguments(fragmentArgs);
    }

    @Test
    public void shouldPerformPlaylistTagSearchWithTagFromBundleInOnCreate() throws Exception {
        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(PlaylistItem.from(playlist)));
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PlaylistItem clickedPlaylist = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onItemClicked(view, 0);

        verify(navigator).openPlaylist(fragmentRule.getActivity(), clickedPlaylist.getEntityUrn(), Screen.SEARCH_PLAYLIST_DISCO);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistTagResultsIsClicked() throws Exception {
        when(adapter.getItem(0)).thenReturn(ModelFixtures.create(PlaylistItem.class));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onItemClicked(view, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(event.getAttributes().get("context")).isEqualTo("tags");
    }

    @Test
    public void shouldTrackSearchTagsScreenOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), null);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.get(ScreenEvent.KEY_SCREEN)).isEqualTo(Screen.SEARCH_PLAYLIST_DISCO.get());
    }
}
