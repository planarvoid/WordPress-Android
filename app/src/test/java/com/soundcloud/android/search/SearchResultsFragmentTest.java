package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.android.OperationPaged;

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
    SearchResultsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment = new SearchResultsFragment(searchOperations, playbackOperations, imageOperations, adapter);
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
    }

    @Test
    public void shouldGetAllResultsForAllQueryOnCreate() throws Exception {
        fragment.setArguments(buildSearchArgs("skrillex", 0));

        fragment.onCreate(null);

        verify(searchOperations).getAllSearchResults("skrillex");
    }

    @Test
    public void shouldGetTracksResultsForTracksQueryOnCreate() throws Exception {
        fragment.setArguments(buildSearchArgs("skrillex", 1));

        fragment.onCreate(null);

        verify(searchOperations).getTrackSearchResults("skrillex");
    }

    @Test
    public void shouldGetPlaylistResultsForPlaylistQueryOnCreate() throws Exception {
        fragment.setArguments(buildSearchArgs("skrillex", 2));

        fragment.onCreate(null);

        verify(searchOperations).getPlaylistSearchResults("skrillex");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypePeopleOnCreate() throws Exception {
        fragment.setArguments(buildSearchArgs("skrillex", 3));

        fragment.onCreate(null);

        verify(searchOperations).getUserSearchResults("skrillex");
    }

    @Test
    public void shouldStartPlaybackWhenClickingPlayableRow() throws Exception {
        adapter.addItem(new Track());

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchEverythingTrackingScreenOnItemClick() throws Exception {
        adapter.addItem(new Track());
        fragment.setArguments(buildSearchArgs("", 0));

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchTracksTrackingScreenOnItemClick() throws Exception {
        adapter.addItem(new Track());
        fragment.setArguments(buildSearchArgs("", 1));

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_TRACKS));
    }

    @Test
    public void shouldSendSearchPlaylistsTrackingScreenOnItemClick() throws Exception {
        adapter.addItem(new Track());
        fragment.setArguments(buildSearchArgs("", 2));

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_PLAYLISTS));
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() throws Exception {
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<OperationPaged.Page<SearchResultsCollection>>error(new Exception()));

        createFragment();
        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        verify(searchOperations, times(2)).getAllSearchResults(anyString());
    }

    @Test
    public void shouldShowErrorStateScreenOnGetResultsError() throws Exception {
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<OperationPaged.Page<SearchResultsCollection>>error(new Exception()));

        createFragment();
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.ERROR);
    }

    @Test
    public void shouldShowWaitingStateWhileLoading() throws Exception {
        // Do not emit items, to simulate an ongoing data fetch
        when(searchOperations.getAllSearchResults(anyString())).
                thenReturn(Observable.<OperationPaged.Page<SearchResultsCollection>>never());

        createFragment();
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    private Bundle buildSearchArgs(String query, int type) {
        Bundle arguments = new Bundle();
        arguments.putString("query", query);
        arguments.putInt("type", type);
        return arguments;
    }

    // HELPERS

    private void createFragment() {
        fragment.setArguments(new Bundle());
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onCreate(null);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
