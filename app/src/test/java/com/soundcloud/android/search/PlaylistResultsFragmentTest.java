package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperationPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.EmptyListView;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.android.OperationPaged;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistResultsFragmentTest {

    private PlaylistResultsFragment fragment;
    private Context context = Robolectric.application;

    @Mock
    private SearchOperations searchOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private PlaylistResultsAdapter adapter;
    @Mock
    private ScModelManager modelManager;

    @Before
    public void setUp() throws Exception {
        createFragment();
    }

    @Test
    public void shouldPerformPlaylistTagSearchInOnCreate() throws Exception {
        PlaylistSummaryCollection collection = new PlaylistSummaryCollection();
        PlaylistSummary playlist = new PlaylistSummary();
        collection.setCollection(Lists.newArrayList(playlist));
        when(searchOperations.getPlaylistDiscoveryResults("selected tag")).thenReturn(
                RxTestHelper.singlePage(Observable.<PlaylistSummaryCollection>from(collection)));

        fragment.onCreate(null);

        ArgumentCaptor<Page> page = ArgumentCaptor.forClass(Page.class);
        verify(adapter).onNext(page.capture());
        expect(page.getValue().getPagedCollection()).toBe(collection);
    }

    @Test
    public void shouldShowErrorStateScreenOnGetResultsError() throws Exception {
        when(searchOperations.getPlaylistDiscoveryResults(anyString())).
                thenReturn(Observable.<Page<PlaylistSummaryCollection>>error(new Exception()));

        fragment.onCreate(null);
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.ERROR);
    }

    @Test
    public void shouldShowWaitingStateWhileLoading() throws Exception {
        // Do not emit items, to simulate an ongoing data fetch
        when(searchOperations.getPlaylistDiscoveryResults(anyString())).
                thenReturn(Observable.<Page<PlaylistSummaryCollection>>never());

        fragment.onCreate(null);
        createFragmentView();

        EmptyListView emptyView = (EmptyListView) fragment.getListView().getEmptyView();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldRecreateObservableWhenClickingRetryAfterFailureSoThatWeDontEmitCachedResults() throws Exception {
        when(searchOperations.getPlaylistDiscoveryResults(anyString())).
                thenReturn(Observable.<OperationPaged.Page<PlaylistSummaryCollection>>error(new Exception()));

        fragment.onCreate(null);
        createFragmentView();

        Button retryButton = (Button) fragment.getView().findViewById(R.id.btn_retry);
        expect(retryButton).not.toBeNull();
        retryButton.performClick();

        verify(searchOperations, times(2)).getPlaylistDiscoveryResults(anyString());
    }

    @Test
    public void shouldUnsubscribeInOnDestroy() {
        Subscription subscription = mock(Subscription.class);
        Observable<Page<PlaylistSummaryCollection>> observable =
                RxTestHelper.<Page<PlaylistSummaryCollection>>mockObservable()
                        .howeverScheduled()
                        .returning(subscription)
                        .get();
        when(searchOperations.getPlaylistDiscoveryResults(anyString())).thenReturn(observable);

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

        fragment.onItemClick(fragment.getListView(), null, 0, 0);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getData()).toEqual(Content.PLAYLISTS.forQuery(String.valueOf(clickedPlaylist.getId())));
    }

    private void createFragment() {
        fragment = new PlaylistResultsFragment(searchOperations, imageOperations, adapter, modelManager);
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
        return layout;
    }

}
