package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

@RunWith(SoundCloudTestRunner.class)
public class PlaylistResultsFragmentTest {

    private Context context = Robolectric.application;
    private AbsListView content;
    private MockObservable<Page<PlaylistSummaryCollection>> observable;

    @InjectMocks
    private PlaylistResultsFragment fragment;

    @Mock
    private SearchOperations searchOperations;
    @Mock
    private ListViewController listViewController;
    @Mock
    private PlaylistResultsAdapter adapter;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private EventBus eventBus;
    @Mock
    private EmptyView emptyView;
    @Mock
    private Subscription subscription;
    @Captor
    private ArgumentCaptor<Page<PlaylistSummaryCollection>> pageCaptor;

    @Before
    public void setUp() throws Exception {
        observable = TestObservables.emptyObservable(subscription);
        when(searchOperations.getPlaylistResults(anyString())).thenReturn(observable);
        when(listViewController.getEmptyView()).thenReturn(emptyView);
        createFragment();
    }

    @Test
    public void shouldPerformPlaylistTagSearchWithTagFromBundleInOnCreate() throws Exception {
        PlaylistSummaryCollection collection = new PlaylistSummaryCollection();
        PlaylistSummary playlist = new PlaylistSummary();
        collection.setCollection(Lists.newArrayList(playlist));
        when(searchOperations.getPlaylistResults("selected tag")).thenReturn(
                RxTestHelper.singlePage(Observable.<PlaylistSummaryCollection>from(collection)));

        fragment.onCreate(null);

        verify(adapter).onNext(pageCaptor.capture());
        expect(pageCaptor.getValue().getPagedCollection()).toBe(collection);
    }

    @Test
    public void shouldAttachListViewControllerInOnViewCreated() {
        fragment.onCreate(null);
        createFragmentView();
        verify(listViewController).onViewCreated(refEq(fragment), any(ConnectableObservable.class),
                refEq(fragment.getView()), refEq(adapter), isA(AbsListViewParallaxer.class));
    }

    @Test
    public void shouldDetachListViewControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(listViewController).onDestroyView();
    }

    @Test
    public void shouldUnsubscribeConnectionSubscriptionInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldOpenPlaylistActivityWhenClickingPlaylistItem() throws CreateModelException {
        PlaylistSummary clickedPlaylist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(Playlist.EXTRA_URN)).toEqual(clickedPlaylist.getUrn());
        expect(Screen.fromIntent(intent)).toBe(Screen.SEARCH_PLAYLIST_DISCO);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistTagResultsIsClicked() throws Exception {
        PlaylistSummary clickedPlaylist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        SearchEvent event = EventMonitor.on(eventBus).verifyEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("tags");
    }

    @Test
    public void shouldTrackSearchTagsScreenOnCreate() {
        createFragment();
        fragment.onCreate(null);

        verify(eventBus).publish(eq(EventQueue.SCREEN_ENTERED), eq(Screen.SEARCH_PLAYLIST_DISCO.get()));
    }

    private void createFragment() {
        Bundle arguments = new Bundle();
        arguments.putString(PlaylistResultsFragment.KEY_PLAYLIST_TAG, "selected tag");
        fragment.setArguments(arguments);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        Robolectric.shadowOf(fragment).setAttached(true);
    }

    private View createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(context), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
        content = (AbsListView) fragment.getView().findViewById(android.R.id.list);
        return layout;
    }

}
