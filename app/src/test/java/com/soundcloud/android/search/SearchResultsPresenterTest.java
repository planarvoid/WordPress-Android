package com.soundcloud.android.search;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchResultsPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(4L);
    private static final Urn USER_URN = Urn.forUser(5L);

    private SearchResultsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private View view;
    @Mock private Fragment fragment;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private Resources resources;
    @Mock private SearchOperations searchOperations;
    @Mock private SearchResultsAdapter adapter;
    @Mock private SearchOperations.SearchPagingFunction searchPagingFunction;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;

    private TestEventBus eventBus = new TestEventBus();
    private TestSubscriber<PlaybackResult> testSubscriber = new TestSubscriber<>();
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Before
    public void setUp() throws Exception {
        presenter = new SearchResultsPresenter(swipeRefreshAttacher, searchOperations, adapter,
                clickListenerFactory, eventBus);

        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, Urn.forTrack(1));
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));

        when(clickListenerFactory.create(any(Screen.class), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        when(searchOperations.pagingFunction(anyInt())).thenReturn(searchPagingFunction);
        when(searchPagingFunction.getSearchQuerySourceInfo(anyInt(), any(Urn.class))).thenReturn(searchQuerySourceInfo);
    }

    @Test
    public void itemClickShouldDelegateToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        final View view = mock(View.class);
        presenter.onBuildBinding(mock(Bundle.class));
        presenter.onItemClicked(view, 0);

        verify(clickListener).onItemClick(listItems, view, 0);
    }
//
//    @Test
//    public void trackItemClickShouldPublishEventFromSearchAllTab() {
//        setupAdapter();
//
//        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);
//
//        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
//        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
//        assertThat(event.getAttributes().get("type")).isEqualTo("track");
//        assertThat(event.getAttributes().get("context")).isEqualTo("everything");
//        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:everything");
//        assertThat(event.getAttributes().get("click_name")).isEqualTo("play");
//        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
//        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
//        assertThat(event.getClickPosition()).isEqualTo(0);
//    }
//
//    @Test
//    public void trackItemClickShouldPublishSearchEventFromTracksTab() {
//        fragment = createFragment(SearchOperations.TYPE_TRACKS, false);
//        fragment.onCreate(null);
//
//        setupAdapter();
//        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);
//
//        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
//        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
//        assertThat(event.getAttributes().get("type")).isEqualTo("track");
//        assertThat(event.getAttributes().get("context")).isEqualTo("tracks");
//        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:tracks");
//        assertThat(event.getAttributes().get("click_name")).isEqualTo("play");
//        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:tracks:1");
//        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
//        assertThat(event.getClickPosition()).isEqualTo(0);
//    }
//
//    @Test
//    public void playlistItemClickShouldPublishSearchEventFromPlaylistTab() {
//        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 1, PLAYLIST_URN);
//        fragment = createFragment(SearchOperations.TYPE_PLAYLISTS, false);
//        fragment.onCreate(null);
//        when(adapter.getItem(1)).thenReturn(PlaylistItem.from(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN))));
//        when(pager.getSearchQuerySourceInfo(1, PLAYLIST_URN)).thenReturn(searchQuerySourceInfo);
//
//        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 1, 0);
//
//        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
//        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
//        assertThat(event.getAttributes().get("type")).isEqualTo("playlist");
//        assertThat(event.getAttributes().get("context")).isEqualTo("playlists");
//        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:playlists");
//        assertThat(event.getAttributes().get("click_name")).isEqualTo("open_playlist");
//        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:playlists:4");
//        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
//        assertThat(event.getClickPosition()).isEqualTo(1);
//    }
//
//    @Test
//    public void userItemClickShouldPublishSearchEventFromUsersTab() {
//        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, USER_URN);
//
//        fragment = createFragment(SearchOperations.TYPE_USERS, false);
//        fragment.onCreate(null);
//        when(adapter.getItem(0)).thenReturn(UserItem.from(PropertySet.from(UserProperty.URN.bind(USER_URN))));
//        when(pager.getSearchQuerySourceInfo(0, USER_URN)).thenReturn(searchQuerySourceInfo);
//
//        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);
//
//        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
//        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_RESULTS);
//        assertThat(event.getAttributes().get("type")).isEqualTo("user");
//        assertThat(event.getAttributes().get("context")).isEqualTo("people");
//        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:people");
//        assertThat(event.getAttributes().get("click_name")).isEqualTo("open_profile");
//        assertThat(event.getAttributes().get("click_object")).isEqualTo("soundcloud:users:5");
//        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
//        assertThat(event.getClickPosition()).isEqualTo(0);
//    }
//
//    @Test
//    public void shouldTrackFirstSearchWithQueryUrn() {
//        fragment = createFragment(SearchOperations.TYPE_ALL, true);
//
//        final List<PropertySetSource> items = Collections.emptyList();
//        final Observable<SearchResult> observable = Observable.just(new SearchResult(items, null, null));
//
//        when(operations.searchResult(eq("query"), anyInt())).thenReturn(observable);
//        when(pager.page(observable)).thenReturn(observable);
//        when(pager.getSearchQuerySourceInfo()).thenReturn(searchQuerySourceInfo);
//
//        fragment.onCreate(null);
//        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
//
//        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_SUBMIT);
//        assertThat(event.getAttributes().get("click_name")).isEqualTo("search");
//        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:everything");
//        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
//    }
//
//    @Test
//    public void shouldNotTrackNonFirstSearch() {
//        fragment = createFragment(SearchOperations.TYPE_ALL, false);
//
//        final List<PropertySetSource> items = Collections.emptyList();
//        final Observable<SearchResult> observable = Observable.just(new SearchResult(items, null, null));
//
//        when(operations.searchResult(eq("query"), anyInt())).thenReturn(observable);
//        when(pager.page(observable)).thenReturn(observable);
//        when(pager.getSearchQuerySourceInfo()).thenReturn(searchQuerySourceInfo);
//
//        fragment.onCreate(null);
//
//        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
//    }
//
//    private SearchResultsFragment createFragment(int searchType, boolean fromSearch) {
//        SearchResultsFragment fragment = new SearchResultsFragment(
//                operations, listViewController, adapter, TestSubscribers.expandPlayerSubscriber(),
//                eventBus, pager, navigator, clickListenerFactory);
//
//        Bundle bundle = new Bundle();
//        bundle.putInt(SearchResultsFragment.EXTRA_TYPE, searchType);
//        bundle.putString(SearchResultsFragment.EXTRA_QUERY, "query");
//        if (fromSearch) {
//            bundle.putBoolean(SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, true);
//        }
//        fragment.setArguments(bundle);
//
//        SupportFragmentTestUtil.startFragment(fragment);
//
//        return fragment;
//    }
//
//    private List<ListItem> setupAdapter() {
//        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
//        final List<ListItem> listItems = Arrays.asList((ListItem) trackItem);
//        when(adapter.getItem(0)).thenReturn(trackItem);
//        when(adapter.getItems()).thenReturn(listItems);
//        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
//        return listItems;
//    }
//
//    private void setupSearchOperations() {
//        when(operations.searchResult(eq("query"), anyInt())).thenReturn(subject);
//        when(operations.pager(anyInt())).thenReturn(pager);
//        when(pager.page(subject)).thenReturn(subject);
//        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
//    }


//    @Test
//    public void trackChangedForNewQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
//        final Urn playingTrack = Urn.forTrack(123L);
//        adapter.onViewCreated(fragment, null, null);
//
//        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack, Urn.NOT_SET, 0));
//
//        verify(trackRenderer).setPlayingTrack(playingTrack);
//    }

//    @Test
//    public void trackChangedForPositionChangedEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
//        final Urn playingTrack = Urn.forTrack(123L);
//        adapter.onViewCreated(fragment, null, null);
//
//        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 0));
//        verify(trackRenderer).setPlayingTrack(playingTrack);
//    }

//    @Test
//    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() {
//        PropertySet unlikedPlaylist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
//        unlikedPlaylist.put(PlaylistProperty.IS_LIKED, false);
//        adapter.addItem(dummyUserItem());
//        adapter.addItem(dummyTrackItem());
//        adapter.addItem(PlaylistItem.from(unlikedPlaylist));
//        adapter.onViewCreated(fragment, null, null);
//
//        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
//                EntityStateChangedEvent.fromLike(unlikedPlaylist.get(PlayableProperty.URN), true, 1));
//
//        final int playlistPosition = 2;
//        adapter.getView(playlistPosition, itemView, new FrameLayout(context()));
//
//        verify(playlistRenderer).bindItemView(eq(playlistPosition), refEq(itemView), playlistItemCaptor.capture());
//        assertThat(playlistItemCaptor.getValue().get(playlistPosition).isLiked()).isTrue();
//    }

//    @Test
//    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
//        adapter.onViewCreated(fragment, null, null);
//        adapter.onDestroyView(fragment);
//        eventBus.verifyUnsubscribed();
//    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Collections.singletonList((ListItem) trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(searchPagingFunction.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
        return listItems;
    }
}