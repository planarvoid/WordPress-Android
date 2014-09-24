package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SearchResultsCollection;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.observables.ConnectableObservable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LegacySearchResultsFragmentTest {

    @InjectMocks private LegacySearchResultsFragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private LegacySearchOperations searchOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ListViewController listViewController;
    @Mock private LegacySearchResultsAdapter adapter;
    @Mock private FragmentActivity activity;
    @Captor private ArgumentCaptor<Intent> intentCaptor;

    @Before
    public void setUp() throws Exception {
        fragment.eventBus = eventBus;
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);
        when(searchOperations.getAllSearchResults(anyString())).thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(listViewController.getEmptyView()).thenReturn(mock(EmptyView.class));
        when(playbackOperations.playTracks(any(List.class), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.<List<Urn>>empty());
    }

    @Test
    public void shouldGetAllResultsForAllQueryOnCreate() throws Exception {
        createWithArguments(buildSearchArgs("skrillex", LegacySearchResultsFragment.TYPE_ALL));
        createFragmentView();

        verify(searchOperations).getAllSearchResults("skrillex");
    }

    @Test
    public void shouldGetTracksResultsForTracksQueryOnCreate() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", LegacySearchResultsFragment.TYPE_TRACKS));
        createFragmentView();

        verify(searchOperations).getTrackSearchResults("skrillex");
    }

    @Test
    public void shouldGetPlaylistResultsForPlaylistQueryOnCreate() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", LegacySearchResultsFragment.TYPE_PLAYLISTS));
        createFragmentView();

        verify(searchOperations).getPlaylistSearchResults("skrillex");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypePeopleOnCreate() throws Exception {
        when(searchOperations.getUserSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", LegacySearchResultsFragment.TYPE_USERS));
        createFragmentView();

        verify(searchOperations).getUserSearchResults("skrillex");
    }

    @Test
    public void shouldConnectListViewControllerInOnViewCreated() {
        createWithArguments(buildSearchArgs("skrillex", LegacySearchResultsFragment.TYPE_ALL));
        createFragmentView();

        verify(listViewController).connect(refEq(fragment), any(ConnectableObservable.class));
    }

    @Test
    public void shouldForwardOnViewCreatedToAdapter() {
        createFragmentView();
        verify(adapter).onViewCreated();
    }

    @Test
    public void shouldForwardOnDestroyViewToAdapter() {
        createFragmentView();
        fragment.onDestroyView();
        verify(adapter).onDestroyView();
    }

    @Test
    public void shouldStartPlaybackWhenClickingPlayableRow() throws Exception {
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playTracks(anyList(), eq(0), eq(new PlaySessionSource(Screen.SEARCH_EVERYTHING)));
    }

    @Test
    public void shouldSendSearchEverythingTrackingScreenOnItemClick() throws Exception {
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_ALL));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playTracks(anyList(), eq(0), eq(new PlaySessionSource(Screen.SEARCH_EVERYTHING)));
    }

    @Test
    public void shouldSendSearchTracksTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playTracks(anyList(), eq(0), eq(new PlaySessionSource(Screen.SEARCH_TRACKS)));
    }

    @Test
    public void shouldSendSearchPlaylistsTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString())).thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        final PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        when(adapter.getItem(0)).thenReturn(playlist);
        when(adapter.getItems()).thenReturn(Arrays.asList(((PublicApiResource) playlist)));

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_PLAYLISTS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(activity).startActivity(intentCaptor.capture());
        expect(intentCaptor.getValue().getAction()).toBe(Actions.PLAYLIST);
        expect(intentCaptor.getValue().getParcelableExtra(PublicApiPlaylist.EXTRA_URN)).toBe(playlist.getUrn());
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnTracksTabIsClicked() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = eventBus.lastEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("tracks");
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistsTabIsClicked() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString())) .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        final PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        when(adapter.getItem(anyInt())).thenReturn(playlist);
        when(adapter.getItems()).thenReturn(Arrays.asList(((PublicApiResource) playlist)));

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_PLAYLISTS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = eventBus.lastEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("playlists");
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPeopleTabIsClicked() throws Exception {
        when(searchOperations.getUserSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new PublicApiUser());

        createWithArguments(buildSearchArgs("", LegacySearchResultsFragment.TYPE_USERS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = eventBus.lastEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("user");
        expect(event.getAttributes().get("context")).toEqual("people");
    }

    private void createWithArguments(Bundle arguments) {
        fragment.setArguments(arguments);
        fragment.onCreate(null);
    }

    private Bundle buildSearchArgs(String query, int type) {
        Bundle arguments = new Bundle();
        arguments.putString("query", query);
        arguments.putInt("type", type);
        return arguments;
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        final Bundle defaultArguments = buildSearchArgs("", LegacySearchResultsFragment.TYPE_ALL);
        fragment.setArguments(defaultArguments);
        fragment.onCreate(null);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
