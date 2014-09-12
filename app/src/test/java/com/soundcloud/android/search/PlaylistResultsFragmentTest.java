package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistCollection;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
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
    private TestEventBus eventBus = new TestEventBus();

    @InjectMocks
    private PlaylistResultsFragment fragment;

    @Mock private SearchOperations searchOperations;
    @Mock private ListViewController listViewController;
    @Mock private PagingItemAdapter<ApiPlaylist> adapter;
    @Mock private ScModelManager modelManager;
    @Mock private EmptyView emptyView;
    @Mock private Subscription subscription;
    @Captor private ArgumentCaptor<Page<ApiPlaylistCollection>> pageCaptor;

    @Before
    public void setUp() throws Exception {
        fragment.eventBus = eventBus;
        Observable<Page<ApiPlaylistCollection>> observable = TestObservables.emptyObservable(subscription);
        when(searchOperations.getPlaylistResults(anyString())).thenReturn(observable);
        when(listViewController.getEmptyView()).thenReturn(emptyView);
        createFragment();
    }

    @Test
    public void shouldPerformPlaylistTagSearchWithTagFromBundleInOnCreate() throws Exception {
        ApiPlaylistCollection collection = new ApiPlaylistCollection();
        ApiPlaylist playlist = new ApiPlaylist();
        collection.setCollection(Lists.newArrayList(playlist));
        when(searchOperations.getPlaylistResults("selected tag")).thenReturn(
                RxTestHelper.singlePage(Observable.<ApiPlaylistCollection>from(collection)));

        fragment.onCreate(null);

        verify(adapter).onNext(pageCaptor.capture());
        expect(pageCaptor.getValue().getPagedCollection()).toBe(collection);
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
        ApiPlaylist clickedPlaylist = ModelFixtures.create(ApiPlaylist.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PublicApiPlaylist.EXTRA_URN)).toEqual(clickedPlaylist.getUrn());
        expect(Screen.fromIntent(intent)).toBe(Screen.SEARCH_PLAYLIST_DISCO);
    }

    @Test
    public void shouldPublishSearchEventWhenResultOnPlaylistTagResultsIsClicked() throws Exception {
        ApiPlaylist clickedPlaylist = ModelFixtures.create(ApiPlaylist.class);
        when(adapter.getItem(0)).thenReturn(clickedPlaylist);

        fragment.onCreate(null);
        createFragmentView();

        fragment.onItemClick(content, null, 0, 0);

        SearchEvent event = eventBus.lastEventOn(EventQueue.SEARCH);
        expect(event.getKind()).toEqual(SearchEvent.SEARCH_RESULTS);
        expect(event.getAttributes().get("type")).toEqual("playlist");
        expect(event.getAttributes().get("context")).toEqual("tags");
    }

    @Test
    public void shouldTrackSearchTagsScreenOnCreate() {
        createFragment();
        fragment.onCreate(null);

        expect(eventBus.lastEventOn(EventQueue.SCREEN_ENTERED)).toEqual(Screen.SEARCH_PLAYLIST_DISCO.get());
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
