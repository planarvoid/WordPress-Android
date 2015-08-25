package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchResultsFragmentTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(4L);
    private static final Urn USER_URN = Urn.forUser(5L);

    private SearchResultsFragment fragment;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SearchOperations operations;
    @Mock private SearchOperations.SearchResultPager pager;
    @Mock private ListViewController listViewController;
    @Mock private SearchResultsAdapter adapter;
    @Mock private Navigator navigator;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;

    private PublishSubject subject = PublishSubject.create();

    @Before
    public void setUp() {
        when(listViewController.getEmptyView()).thenReturn(new EmptyView(context()));
        setupSearchOperations();

        fragment = createFragment(SearchOperations.TYPE_ALL, false);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, Urn.forTrack(1));
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));

        when(clickListenerFactory.create(any(Screen.class), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
    }

    @Test
    public void shouldUnsubscribeFromSourceObservableInOnDestroy() {
        fragment.onDestroy();
        assertThat(subject.hasObservers()).isFalse();
    }

    @Test
    public void itemClickShouldDelegateToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        final View view = mock(View.class);
        fragment.onItemClick(mock(AdapterView.class), view, 0, 0);

        verify(clickListener).onItemClick(listItems, view, 0);
    }

    @Test
    public void trackItemClickShouldPublishEventFromSearchAllTab() {
        setupAdapter();

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

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
    public void trackItemClickShouldPublishSearchEventFromTracksTab() {
        fragment = createFragment(SearchOperations.TYPE_TRACKS, false);
        fragment.onCreate(null);

        setupAdapter();
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

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
    public void playlistItemClickShouldPublishSearchEventFromPlaylistTab() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 1, PLAYLIST_URN);
        fragment = createFragment(SearchOperations.TYPE_PLAYLISTS, false);
        fragment.onCreate(null);
        when(adapter.getItem(1)).thenReturn(PlaylistItem.from(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN))));
        when(pager.getSearchQuerySourceInfo(1, PLAYLIST_URN)).thenReturn(searchQuerySourceInfo);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 1, 0);

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
    public void userItemClickShouldPublishSearchEventFromUsersTab() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, USER_URN);

        fragment = createFragment(SearchOperations.TYPE_USERS, false);
        fragment.onCreate(null);
        when(adapter.getItem(0)).thenReturn(UserItem.from(PropertySet.from(UserProperty.URN.bind(USER_URN))));
        when(pager.getSearchQuerySourceInfo(0, USER_URN)).thenReturn(searchQuerySourceInfo);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

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
    public void shouldTrackFirstSearchWithQueryUrn() {
        fragment = createFragment(SearchOperations.TYPE_ALL, true);

        final List<PropertySetSource> items = Collections.emptyList();
        final Observable<SearchResult> observable = Observable.just(new SearchResult(items, null, null));

        when(operations.searchResult(eq("query"), anyInt())).thenReturn(observable);
        when(pager.page(observable)).thenReturn(observable);
        when(pager.getSearchQuerySourceInfo()).thenReturn(searchQuerySourceInfo);

        fragment.onCreate(null);
        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);

        assertThat(event.getKind()).isEqualTo(SearchEvent.KIND_SUBMIT);
        assertThat(event.getAttributes().get("click_name")).isEqualTo("search");
        assertThat(event.getAttributes().get("page_name")).isEqualTo("search:everything");
        assertThat(event.getAttributes().get("query_urn")).isEqualTo("soundcloud:search:123");
    }

    @Test
    public void shouldNotTrackNonFirstSearch() {
        fragment = createFragment(SearchOperations.TYPE_ALL, false);

        final List<PropertySetSource> items = Collections.emptyList();
        final Observable<SearchResult> observable = Observable.just(new SearchResult(items, null, null));

        when(operations.searchResult(eq("query"), anyInt())).thenReturn(observable);
        when(pager.page(observable)).thenReturn(observable);
        when(pager.getSearchQuerySourceInfo()).thenReturn(searchQuerySourceInfo);

        fragment.onCreate(null);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    private SearchResultsFragment createFragment(int searchType, boolean fromSearch) {
        SearchResultsFragment fragment = new SearchResultsFragment(
                operations, listViewController, adapter, TestSubscribers.expandPlayerSubscriber(),
                eventBus, pager, navigator, clickListenerFactory);

        Bundle bundle = new Bundle();
        bundle.putInt(SearchResultsFragment.EXTRA_TYPE, searchType);
        bundle.putString(SearchResultsFragment.EXTRA_QUERY, "query");
        if (fromSearch) {
            bundle.putBoolean(SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, true);
        }
        fragment.setArguments(bundle);

        SupportFragmentTestUtil.startFragment(fragment);

        return fragment;
    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Arrays.asList((ListItem) trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
        return listItems;
    }

    private void setupSearchOperations() {
        when(operations.searchResult(eq("query"), anyInt())).thenReturn(subject);
        when(operations.pager(anyInt())).thenReturn(pager);
        when(pager.page(subject)).thenReturn(subject);
        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
    }
}