package com.soundcloud.android.search;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    private SearchResultsFragment fragment;

    @Mock
    SearchOperations searchOperations;

    @Mock
    PlaybackOperations playbackOperations;

    @Mock
    SearchResultsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment = new SearchResultsFragment(searchOperations, playbackOperations, adapter);
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypeAllOnCreate() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("query", "a query");
        arguments.putInt("type", 0);
        fragment.setArguments(arguments);

        fragment.onCreate(null);

        verify(searchOperations).getSearchResultsAll("a query");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypeTracksOnCreate() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("query", "a query");
        arguments.putInt("type", 1);
        fragment.setArguments(arguments);

        fragment.onCreate(null);

        verify(searchOperations).getSearchResultsTracks("a query");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypePlaylistsOnCreate() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("query", "a query");
        arguments.putInt("type", 2);
        fragment.setArguments(arguments);

        fragment.onCreate(null);

        verify(searchOperations).getSearchResultsPlaylists("a query");
    }

    @Test
    public void shouldGetSearchAllResultsForQueryTypePeopleOnCreate() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("query", "a query");
        arguments.putInt("type", 3);
        fragment.setArguments(arguments);

        fragment.onCreate(null);

        verify(searchOperations).getSearchResultsPeople("a query");
    }

    @Test
    public void shouldStartPlaybackWhenClickingPlayableRow() throws Exception {
        Track track = mock(Track.class);
        adapter.addItem(track);

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchEverythingTrackingScreenOnItemClick() throws Exception {
        Track track = mock(Track.class);
        adapter.addItem(track);
        Bundle arguments = new Bundle();
        arguments.putInt("type", 0);
        fragment.setArguments(arguments);

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_EVERYTHING));
    }

    @Test
    public void shouldSendSearchTracksTrackingScreenOnItemClick() throws Exception {
        Track track = mock(Track.class);
        adapter.addItem(track);
        Bundle arguments = new Bundle();
        arguments.putInt("type", 1);
        fragment.setArguments(arguments);

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_TRACKS));
    }

    @Test
    public void shouldSendSearchPlaylistsTrackingScreenOnItemClick() throws Exception {
        Track track = mock(Track.class);
        adapter.addItem(track);
        Bundle arguments = new Bundle();
        arguments.putInt("type", 2);
        fragment.setArguments(arguments);

        fragment.onCreate(null);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        verify(playbackOperations).playFromAdapter(any(Context.class), anyList(), eq(0), isNull(Uri.class), eq(Screen.SEARCH_PLAYLISTS));
    }

}
