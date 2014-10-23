package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.buildProvider;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackProperty;
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(4L);
    private static final Urn USER_URN = Urn.forUser(5L);

    private SearchResultsFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private SearchOperations operations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private SearchOperations.SearchResultPager pager;
    @Mock private Subscription subscription;
    @Mock private ListViewController listViewController;
    @Mock private SearchResultsAdapter adapter;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    @Before
    public void setUp() {
        setupSearchOperations();
        fragment = createFragment(SearchOperations.TYPE_ALL);
    }

    @Test
    public void shouldUnsubscribeFromSourceObservableInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void trackItemClickShouldPlayTrack() {
        TestObservables.MockObservable<List<Urn>> playbackObservable =
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
    }

    @Test
    public void trackItemClickShouldPlayTrackFromTracksTab() {
        fragment = createFragment(SearchOperations.TYPE_TRACKS);
        fragment.onCreate(null);

        TestObservables.MockObservable<List<Urn>> playbackObservable =
                setupAdapterAndPlaybackOperations(Screen.SEARCH_TRACKS);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        expect(playbackObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void trackItemClickShouldPublishSearchEventFromTracksTab() {
        fragment = createFragment(SearchOperations.TYPE_TRACKS);
        fragment.onCreate(null);

        setupAdapterAndPlaybackOperations(Screen.SEARCH_TRACKS);
        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("track");
        expect(event.getAttributes().get("context")).toEqual("tracks");
    }

    @Test
    public void playlistItemClickShouldOpenPlaylistActivity() {
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN)));

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        Intent nextStartedActivity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getAction()).toEqual(Actions.PLAYLIST);
        expect(nextStartedActivity.getExtras().get(PlaylistDetailActivity.EXTRA_URN)).toEqual(PLAYLIST_URN);
    }

    @Test
    public void playlistItemClickShouldPublishSearchEventFromPlaylistTab() {
        fragment = createFragment(SearchOperations.TYPE_PLAYLISTS);
        fragment.onCreate(null);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(PlayableProperty.URN.bind(PLAYLIST_URN)));

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("playlists");
    }

    @Test
    public void userItemClickShouldOpenProfileActivity() {
        when(adapter.getItem(0)).thenReturn(PropertySet.from(UserProperty.URN.bind(USER_URN)));

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        Intent nextStartedActivity = Robolectric.shadowOf(fragment.getActivity()).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(nextStartedActivity.getExtras().get(ProfileActivity.EXTRA_USER_URN)).toEqual(USER_URN);
    }

    @Test
    public void userItemClickShouldPublishSearchEventFromUsersTab() {
        fragment = createFragment(SearchOperations.TYPE_USERS);
        fragment.onCreate(null);
        when(adapter.getItem(0)).thenReturn(PropertySet.from(UserProperty.URN.bind(USER_URN)));

        fragment.onItemClick(mock(AdapterView.class), mock(View.class), 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("user");
        expect(event.getAttributes().get("context")).toEqual("people");
    }

    private SearchResultsFragment createFragment(int searchType) {
        SearchResultsFragment fragment = new SearchResultsFragment(
                operations, playbackOperations, listViewController, adapter, buildProvider(expandPlayerSubscriber), eventBus);

        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());

        Bundle bundle = new Bundle();
        bundle.putInt(SearchResultsFragment.EXTRA_TYPE, searchType);
        bundle.putString(SearchResultsFragment.EXTRA_QUERY, "query");
        fragment.setArguments(bundle);
        return fragment;
    }

    private TestObservables.MockObservable<List<Urn>> setupAdapterAndPlaybackOperations(Screen screen) {
        TestObservables.MockObservable<List<Urn>> playbackObservable = TestObservables.emptyObservable();
        when(adapter.getItem(0)).thenReturn(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        when(adapter.getItems()).thenReturn(Lists.<PropertySet>newArrayList(PropertySet.from(TrackProperty.URN.bind(TRACK_URN))));
        when(playbackOperations
                .playTracks(eq(Lists.newArrayList(TRACK_URN)), eq(TRACK_URN), eq(0), eq(new PlaySessionSource(screen))))
                .thenReturn(playbackObservable);
        return playbackObservable;
    }

    private void setupSearchOperations() {
        final Observable<SearchResult> searchResult =
                TestObservables.withSubscription(subscription, Observable.<SearchResult>never());
        when(operations.searchResult(eq("query"), anyInt())).thenReturn(searchResult);
        when(operations.pager(anyInt())).thenReturn(pager);
        when(pager.page(searchResult)).thenReturn(searchResult);
    }

}