package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchResultsPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(4L);
    private static final Urn USER_URN = Urn.forUser(5L);

    private SearchResultsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SearchOperations searchOperations;
    @Mock private SearchResultsAdapter adapter;
    @Mock private SearchOperations.SearchPagingFunction searchPagingFunction;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;
    @Mock private TrackItemRenderer trackItemRenderer;

    private TestEventBus eventBus = new TestEventBus();
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh, new Bundle());

    @Before
    public void setUp() throws Exception {
        presenter = new SearchResultsPresenter(swipeRefreshAttacher, searchOperations, adapter,
                clickListenerFactory, eventBus);

        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, Urn.forTrack(1));
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));

        when(clickListenerFactory.create(any(Screen.class), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        when(searchOperations.searchResult(anyString(), anyInt())).thenReturn(Observable.<SearchResult>empty());
        when(searchOperations.pagingFunction(anyInt())).thenReturn(searchPagingFunction);
        when(searchPagingFunction.getSearchQuerySourceInfo(anyInt(), any(Urn.class))).thenReturn(searchQuerySourceInfo);
    }

    @Test
    public void itemClickDelegatesToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 0);

        verify(clickListener).onItemClick(listItems, fragmentRule.getView(), 0);
    }

    @Test
    public void trackItemClickPublishesEventFromSearchAllTab() {
        setupAdapter();

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("track");
        assertThat(event.getAttributes().get("context")).isEqualTo("everything");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:everything");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("play");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void trackItemClickPublishesSearchEventFromTracksTab() {
        final Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_TYPE, SearchOperations.TYPE_TRACKS);
        arguments.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, false);

        setupAdapter();

        presenter.onBuildBinding(arguments);
        presenter.onItemClicked(fragmentRule.getView(), 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("track");
        assertThat(event.getAttributes().get("context")).isEqualTo("tracks");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:tracks");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("play");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void playlistItemClickPublishesSearchEventFromPlaylistTab() {
        final Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_TYPE, SearchOperations.TYPE_PLAYLISTS);
        arguments.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, false);

        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 1, PLAYLIST_URN);

        when(adapter.getItem(1)).thenReturn(PlaylistItem.from(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN))));
        when(searchPagingFunction.getSearchQuerySourceInfo(1, PLAYLIST_URN)).thenReturn(searchQuerySourceInfo);

        presenter.onBuildBinding(arguments);
        presenter.onItemClicked(fragmentRule.getView(), 1);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("playlist");
        assertThat(event.getAttributes().get("context")).isEqualTo("playlists");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:playlists");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("open_playlist");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:playlists:4");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(1);
    }

    @Test
    public void userItemClickPublishesSearchEventFromUsersTab() {
        final Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_TYPE, SearchOperations.TYPE_USERS);
        arguments.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, false);

        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, USER_URN);

        when(adapter.getItem(0)).thenReturn(UserItem.from(PropertySet.from(UserProperty.URN.bind(USER_URN))));
        when(searchPagingFunction.getSearchQuerySourceInfo(0, USER_URN)).thenReturn(searchQuerySourceInfo);

        presenter.onBuildBinding(arguments);
        presenter.onItemClicked(fragmentRule.getView(), 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
        assertThat(event.getAttributes().get("type")).isEqualTo("user");
        assertThat(event.getAttributes().get("context")).isEqualTo("people");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:people");
        assertThat(event.getAttributes().get("click_name")).isEqualTo("open_profile");
        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:users:5");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
        assertThat(event.getClickPosition()).isEqualTo(0);
    }

    @Test
    public void doesNotTrackNonFirstSearch() {
        final Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_TYPE, SearchOperations.TYPE_ALL);
        arguments.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, false);

        final List<PropertySetSource> items = Collections.emptyList();
        final Observable<SearchResult> observable = Observable.just(new SearchResult(items, null, null));

        when(searchOperations.searchResult(eq("query"), anyInt())).thenReturn(observable);
        when(searchPagingFunction.call(any(SearchResult.class))).thenReturn(observable);
        when(searchPagingFunction.getSearchQuerySourceInfo()).thenReturn(searchQuerySourceInfo);

        presenter.onBuildBinding(arguments);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void trackChangedForNewQueueEventUpdatesTrackPresenterWithCurrentlyPlayingTrack() {
        when(adapter.getTrackRenderer()).thenReturn(trackItemRenderer);

        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack, Urn.NOT_SET, 0));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void trackChangedForPositionChangedEventUpdatesTrackPresenterWithCurrentlyPlayingTrack() {
        when(adapter.getTrackRenderer()).thenReturn(trackItemRenderer);

        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 0));

        verify(trackItemRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroyView() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Collections.singletonList((ListItem) trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(searchPagingFunction.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
        return listItems;
    }
}