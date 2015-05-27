package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(4L);
    private static final Urn USER_URN = Urn.forUser(5L);

    private SearchResultsFragment fragment;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SearchOperations operations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private SearchOperations.SearchResultPager pager;
    @Mock private Subscription subscription;
    @Mock private ListViewController listViewController;
    @Mock private SearchResultsAdapter adapter;
    @Mock private Navigator navigator;

    @Before
    public void setUp() {
        setupSearchOperations();
        fragment = createFragment(SearchOperations.TYPE_ALL, false);
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:123"), 0, Urn.forTrack(1));
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));
    }

    @Test
    public void shouldUnsubscribeFromSourceObservableInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void trackItemClickShouldPlayTrack() {
        TestObservables.MockObservable<PlaybackResult> playbackObservable =
                setupAdapterAndPlaybackOperations(Screen.SEARCH_EVERYTHING);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        expect(playbackObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void trackItemClickShouldPublishEventFromSearchAllTab() {
        setupAdapterAndPlaybackOperations(Screen.SEARCH_EVERYTHING);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("everything");
        expect(event.getAttributes().get("page_name")).toEqual("search:everything");
        expect(event.getAttributes().get("click_name")).toEqual("play");
        expect(event.getAttributes().get("click_object")).toEqual("soundcloud:tracks:1");
        expect(event.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(event.getAttributes().get("click_position")).toEqual("0");
    }

    @Test
    public void trackItemClickShouldPlayTrackFromTracksTab() {
        fragment = createFragment(SearchOperations.TYPE_TRACKS, false);
        fragment.onCreate(null);

        TestObservables.MockObservable<PlaybackResult> playbackObservable =
                setupAdapterAndPlaybackOperations(Screen.SEARCH_TRACKS);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        expect(playbackObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void trackItemClickShouldPublishSearchEventFromTracksTab() {
        fragment = createFragment(SearchOperations.TYPE_TRACKS, false);
        fragment.onCreate(null);

        setupAdapterAndPlaybackOperations(Screen.SEARCH_TRACKS);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("tracks");
        expect(event.getAttributes().get("page_name")).toEqual("search:tracks");
        expect(event.getAttributes().get("click_name")).toEqual("play");
        expect(event.getAttributes().get("click_object")).toEqual("soundcloud:tracks:1");
        expect(event.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(event.getAttributes().get("click_position")).toEqual("0");
    }

    @Test
    public void playlistItemClickShouldOpenPlaylistActivity() {
        when(adapter.getItem(0)).thenReturn(PlaylistItem.from(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN))));
        when(pager.getSearchQuerySourceInfo(0, PLAYLIST_URN)).thenReturn(searchQuerySourceInfo);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        Intent nextStartedActivity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getAction()).toEqual(Actions.PLAYLIST);
        expect(nextStartedActivity.getExtras().get(PlaylistDetailActivity.EXTRA_URN))
                .toEqual(ParcelableUrn.from(PLAYLIST_URN));
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
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("playlists");
        expect(event.getAttributes().get("page_name")).toEqual("search:playlists");
        expect(event.getAttributes().get("click_name")).toEqual("open_playlist");
        expect(event.getAttributes().get("click_object")).toEqual("soundcloud:playlists:4");
        expect(event.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(event.getAttributes().get("click_position")).toEqual("1");
    }

    @Test
    public void userItemClickShouldOpenProfileActivity() {
        when(adapter.getItem(0)).thenReturn(UserItem.from(PropertySet.from(UserProperty.URN.bind(USER_URN))));
        when(pager.getSearchQuerySourceInfo(0, USER_URN)).thenReturn(searchQuerySourceInfo);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(navigator).openProfile(any(Context.class), eq(USER_URN), eq(searchQuerySourceInfo));
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
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("user");
        expect(event.getAttributes().get("context")).toEqual("people");
        expect(event.getAttributes().get("page_name")).toEqual("search:people");
        expect(event.getAttributes().get("click_name")).toEqual("open_profile");
        expect(event.getAttributes().get("click_object")).toEqual("soundcloud:users:5");
        expect(event.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
        expect(event.getAttributes().get("click_position")).toEqual("0");
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

        expect(event.getKind()).toEqual(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("click_name")).toEqual("search");
        expect(event.getAttributes().get("page_name")).toEqual("search:everything");
        expect(event.getAttributes().get("query_urn")).toEqual("soundcloud:search:123");
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
                operations, playbackOperations, listViewController, adapter, TestSubscribers.expandPlayerSubscriber(),
                eventBus, pager, navigator);

        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());

        Bundle bundle = new Bundle();
        bundle.putInt(SearchResultsFragment.EXTRA_TYPE, searchType);
        bundle.putString(SearchResultsFragment.EXTRA_QUERY, "query");
        if (fromSearch) {
            bundle.putBoolean(SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, true);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    private TestObservables.MockObservable<PlaybackResult> setupAdapterAndPlaybackOperations(Screen screen) {
        TestObservables.MockObservable<PlaybackResult> playbackObservable = TestObservables.emptyObservable();
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(Arrays.asList((ListItem) trackItem));
        when(playbackOperations
                .playTracks(eq(Lists.newArrayList(TRACK_URN)), eq(TRACK_URN), eq(0), eq(new PlaySessionSource(screen))))
                .thenReturn(playbackObservable);
        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
        return playbackObservable;
    }

    private void setupSearchOperations() {
        final Observable<SearchResult> searchResult =
                TestObservables.withSubscription(subscription, Observable.<SearchResult>never());
        when(operations.searchResult(eq("query"), anyInt())).thenReturn(searchResult);
        when(operations.pager(anyInt())).thenReturn(pager);
        when(pager.page(searchResult)).thenReturn(searchResult);
        when(pager.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
    }
}