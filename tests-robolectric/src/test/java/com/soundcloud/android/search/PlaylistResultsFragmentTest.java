package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.presentation.PagingItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistResultsFragmentTest {

    private Context context = Robolectric.application;
    private AbsListView content;
    private TestEventBus eventBus = new TestEventBus();

    @InjectMocks private PlaylistResultsFragment fragment;

    @Mock private PlaylistDiscoveryOperations operations;
    @Mock private ListViewController listViewController;
    @Mock private PagingItemAdapter<PlaylistItem> adapter;
    @Mock private EmptyView emptyView;
    @Mock private Subscription subscription;
    @Mock private PlaylistDiscoveryOperations.PlaylistPager pager;

    @Before
    public void setUp() throws Exception {
        fragment.eventBus = eventBus;
        Observable<ApiPlaylistCollection> observable = TestObservables.emptyObservable(subscription);
        when(operations.pager(anyString())).thenReturn(pager);
        when(pager.page(observable)).thenReturn(observable);
        when(operations.playlistsForTag(anyString())).thenReturn(observable);
        when(listViewController.getEmptyView()).thenReturn(emptyView);
        createFragment();
    }

    @Test
    public void shouldPerformPlaylistTagSearchWithTagFromBundleInOnCreate() throws Exception {
        ApiPlaylistCollection collection = new ApiPlaylistCollection();
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        collection.setCollection(Arrays.asList(playlist));
        final Observable<ApiPlaylistCollection> observable = Observable.just(collection);
        when(pager.page(observable)).thenReturn(observable);
        when(operations.playlistsForTag("selected tag")).thenReturn(observable);

        fragment.onCreate(null);

        verify(adapter).onNext(Arrays.asList(PlaylistItem.from(playlist)));
    }

    @Test
    public void shouldConnectListViewControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(listViewController).connect(refEq(fragment), any(ConnectableObservable.class));
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PlaylistItem clickedPlaylist = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(ParcelableUrn.unpack(EXTRA_URN, intent.getExtras())).toEqual(clickedPlaylist.getEntityUrn());
        expect(Screen.fromIntent(intent)).toBe(Screen.SEARCH_PLAYLIST_DISCO);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistTagResultsIsClicked() throws Exception {
        when(adapter.getItem(0)).thenReturn(ModelFixtures.create(PlaylistItem.class));

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("tags");
    }

    @Test
    public void shouldTrackSearchTagsScreenOnCreate() {
        createFragment();
        fragment.onCreate(null);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.get(ScreenEvent.KEY_SCREEN)).toEqual(Screen.SEARCH_PLAYLIST_DISCO.get());
    }

    private void createFragment() {
        Bundle arguments = new Bundle();
        arguments.putString(PlaylistResultsFragment.KEY_PLAYLIST_TAG, "selected tag");
        fragment.setArguments(arguments);
        final FragmentActivity activity = new FragmentActivity();
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onAttach(activity);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(context), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
        content = (AbsListView) fragment.getView().findViewById(android.R.id.list);
        return layout;
    }

}
