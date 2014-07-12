package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.api.legacy.model.SearchResultsCollection;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.observables.ConnectableObservable;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    @InjectMocks
    private SearchResultsFragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private SearchOperations searchOperations;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private ListViewController listViewController;
    @Mock
    private SearchResultsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment.eventBus = eventBus;
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
        Robolectric.shadowOf(fragment).setAttached(true);
        when(listViewController.getEmptyView()).thenReturn(mock(EmptyView.class));
    }

    @Test
    public void shouldGetAllResultsForAllQueryOnCreate() throws Exception {
        when(searchOperations.getAllSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty()); 

        createWithArguments(buildSearchArgs("skrillex", SearchResultsFragment.TYPE_ALL));
        createFragmentView();

        verify(searchOperations).getAllSearchResults("skrillex");
    }

    @Test
    public void shouldGetTracksResultsForTracksQueryOnCreate() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", SearchResultsFragment.TYPE_TRACKS));
        createFragmentView();

        verify(searchOperations).getTrackSearchResults("skrillex");
    }

    @Test
    public void shouldGetPlaylistResultsForPlaylistQueryOnCreate() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", SearchResultsFragment.TYPE_PLAYLISTS));
        createFragmentView();

        verify(searchOperations).getPlaylistSearchResults("skrillex");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypePeopleOnCreate() throws Exception {
        when(searchOperations.getUserSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());

        createWithArguments(buildSearchArgs("skrillex", SearchResultsFragment.TYPE_USERS));
        createFragmentView();

        verify(searchOperations).getUserSearchResults("skrillex");
    }

    @Test
    public void shouldAttachListViewControllerInOnViewCreated() {
        final Observable<Page<SearchResultsCollection>> observable = Observable.empty();
        when(searchOperations.getAllSearchResults(anyString())).thenReturn(observable);

        createWithArguments(buildSearchArgs("skrillex", SearchResultsFragment.TYPE_ALL));
        createFragmentView();

        verify(listViewController).onViewCreated(refEq(fragment), any(ConnectableObservable.class),
                refEq(fragment.getView()), refEq(adapter), refEq(adapter));
    }

    @Test
    public void shouldDetachListViewControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(listViewController).onDestroyView();
    }

    @Test
    public void shouldForwardOnViewCreatedToAdapter() {
        createFragmentView();
        verify(adapter).onViewCreated();
    }

    @Test
    public void shouldForwardOnDestroyViewToAdapter() {
        fragment.onDestroyView();
        verify(adapter).onDestroyView();
    }

    @Test
    public void shouldStartPlaybackWhenClickingPlayableRow() throws Exception {
        when(searchOperations.getAllSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchEverythingTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getAllSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_ALL));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchTracksTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_TRACKS));
    }

    @Test
    public void shouldSendSearchPlaylistsTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new PublicApiPlaylist());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_PLAYLISTS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_PLAYLISTS));
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnTracksTabIsClicked() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new PublicApiTrack());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = eventBus.lastEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("tracks");
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistsTabIsClicked() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new PublicApiPlaylist());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_PLAYLISTS));
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

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_USERS));
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
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
