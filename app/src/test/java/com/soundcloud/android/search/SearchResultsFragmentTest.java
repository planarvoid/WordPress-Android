package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.android.OperatorPaged;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    private SearchResultsFragment fragment;

    @Mock
    SearchOperations searchOperations;
    @Mock
    PlaybackOperations playbackOperations;
    @Mock
    ImageOperations imageOperations;
    @Mock
    EventBus eventBus;
    @Mock
    SearchResultsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment = new SearchResultsFragment(searchOperations, playbackOperations, imageOperations, eventBus, adapter);
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
        Robolectric.shadowOf(fragment).setAttached(true);
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
    public void shouldStartPlaybackWhenClickingPlayableRow() throws Exception {
        when(searchOperations.getAllSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new Track());

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchEverythingTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getAllSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new Track());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_ALL));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchTracksTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new Track());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_TRACKS));
    }

    @Test
    public void shouldSendSearchPlaylistsTrackingScreenOnItemClick() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(0)).thenReturn(new Playlist());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_PLAYLISTS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_PLAYLISTS));
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() throws Exception {
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<Page<SearchResultsCollection>>error(new Exception()));

        createWithArguments(new Bundle());
        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        verify(searchOperations, times(2)).getAllSearchResults(anyString());
    }

    @Test
    public void shouldShowErrorStateScreenOnGetResultsError() throws Exception {
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<Page<SearchResultsCollection>>error(new Exception()));

        createWithArguments(new Bundle());
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.ERROR);
    }

    @Test
    public void shouldShowWaitingStateWhileLoading() throws Exception {
        // Do not emit items, to simulate an ongoing data fetch
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<Page<SearchResultsCollection>>never());

        createWithArguments(new Bundle());
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnTracksTabIsClicked() throws Exception {
        when(searchOperations.getTrackSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new Track());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_TRACKS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = EventMonitor.on(eventBus).verifyEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("tracks");
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistsTabIsClicked() throws Exception {
        when(searchOperations.getPlaylistSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new Playlist());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_PLAYLISTS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = EventMonitor.on(eventBus).verifyEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("playlists");
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPeopleTabIsClicked() throws Exception {
        when(searchOperations.getUserSearchResults(anyString()))
                .thenReturn(Observable.<Page<SearchResultsCollection>>empty());
        when(adapter.getItem(anyInt())).thenReturn(new User());

        createWithArguments(buildSearchArgs("", SearchResultsFragment.TYPE_USERS));
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = EventMonitor.on(eventBus).verifyEventOn(EventQueue.SEARCH);
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
