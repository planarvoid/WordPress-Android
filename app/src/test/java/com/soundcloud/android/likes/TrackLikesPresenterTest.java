package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesPresenterTest {

    private TrackLikesPresenter presenter;

    @Mock private LikeOperations likeOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private PagedTracksAdapter adapter;
    @Mock private TrackLikesActionMenuController actionMenuController;
    @Mock private TrackLikesHeaderPresenter headerPresenter;
    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private Menu menu;
    @Mock private MenuInflater menuInflater;
    @Mock private MenuItem menuItem;

    private List<PropertySet> likedTracks = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
    private PublishSubject<List<PropertySet>> likedTracksObservable = PublishSubject.create();
    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() {
        presenter = new TrackLikesPresenter(likeOperations, playbackOperations,
                adapter, actionMenuController, headerPresenter, expandPlayerSubscriberProvider,
                eventBus, imageOperations, pullToRefreshWrapper);
        when(view.findViewById(android.R.id.list)).thenReturn(listView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(likeOperations.likedTracks()).thenReturn(likedTracksObservable);
        when(likeOperations.likedTracksPager()).thenReturn(RxTestHelper.<List<PropertySet>>pagerWithSinglePage());
    }

    @Test
    public void shouldConnectLikedTracksBindingInOnCreate() {
        presenter.onCreate(fragment, null);
        likedTracksObservable.subscribe(testSubscriber);
        likedTracksObservable.onNext(likedTracks);
        testSubscriber.assertReceivedOnNext(Arrays.asList(likedTracks));
    }

    @Test
    public void shouldOnSubscribeListObserversToHeaderPresenter() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(headerPresenter).onSubscribeListObservers(any(ListBinding.class));
    }

    @Test
    public void shouldRedirectOnCreateOptionsMenuToActionMenuController() {
        presenter.onCreateOptionsMenu(menu, menuInflater);
        verify(actionMenuController).onCreateOptionsMenu(menu, menuInflater);
    }

    @Test
    public void shouldRedirectOnOptionsItemSelectedToActionMenuController() {
        presenter.onOptionsItemSelected(fragment, menuItem);
        verify(actionMenuController).onOptionsItemSelected(fragment, menuItem);
    }

    @Test
    public void shouldRedirectOnResumeToActionMenuController() {
        presenter.onResume(fragment);
        verify(actionMenuController).onResume(fragment);
    }

    @Test
    public void shouldRedirectOnResumeToHeaderPresenter() {
        presenter.onResume(fragment);
        verify(headerPresenter).onResume();
    }

    @Test
    public void shouldRedirectOnPauseToActionMenuController() {
        presenter.onPause(fragment);
        verify(actionMenuController).onPause();
    }

    @Test
    public void shouldRedirectOnPauseToHeaderPresenter() {
        presenter.onPause(fragment);
        verify(headerPresenter).onPause();
    }

    @Test
    public void shouldRedirectOnDestroyViewToHeaderPresenter() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);
        headerPresenter.onDestroyView();
    }

    @Test
    public void shouldRegisterOnItemClickHandlerWithList() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(listView).setOnItemClickListener(presenter);
    }

    @Test
    public void shouldPlayLikedTracksOnListItemClick() {
        PropertySet clickedTrack = TestPropertySets.expectedLikedTrackForLikesScreen();
        when(listView.getItemAtPosition(0)).thenReturn(clickedTrack);
        final List<Urn> likedUrns = Arrays.asList(Urn.forTrack(123));
        final Observable<List<Urn>> likedUrnsObservable = Observable.just(likedUrns);
        when(likeOperations.likedTrackUrns()).thenReturn(likedUrnsObservable);
        when(playbackOperations.playTracks(
                eq(likedUrnsObservable), eq(clickedTrack.get(TrackProperty.URN)), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(likedUrnsObservable);

        presenter.onItemClick(listView, view, 0, 0);

        testSubscriber.assertReceivedOnNext(Arrays.asList(likedUrns));
    }

    @Test
    public void shouldRefreshListContentAfterOfflineQueueUpdateEvent() throws Exception {
        when(likeOperations.likedTracks()).thenReturn(Observable.<List<PropertySet>>empty(), likedTracksObservable);

        presenter.onCreate(fragment, null);
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.queueUpdate());
        likedTracksObservable.onNext(likedTracks);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).onNext(likedTracks);
    }

    @Test
    public void shouldNotRefreshListContentAfterOtherOfflineSyncEvents() throws Exception {
        presenter.onCreate(fragment, null);

        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.start());
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.idle());
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.stop());

        verify(adapter, never()).clear();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroy() {
        presenter.onCreate(fragment, null);
        presenter.onDestroy(fragment);
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.queueUpdate());
        verify(adapter, never()).clear();
    }
}