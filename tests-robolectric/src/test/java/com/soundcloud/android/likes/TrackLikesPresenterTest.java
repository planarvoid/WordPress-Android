package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackOperations;
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

    public static final Urn TRACK_URN = Urn.forTrack(123);

    private TrackLikesPresenter presenter;

    @Mock private LikeOperations likeOperations;
    @Mock private TrackOperations trackOperations;
    @Mock private OfflinePlaybackOperations playbackOperations;
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
        presenter = new TrackLikesPresenter(likeOperations, trackOperations, playbackOperations,
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
        verify(headerPresenter).onResume(fragment);
    }

    @Test
    public void shouldRedirectOnPauseToActionMenuController() {
        presenter.onPause(fragment);
        verify(actionMenuController).onPause(fragment);
    }

    @Test
    public void shouldRedirectOnPauseToHeaderPresenter() {
        presenter.onPause(fragment);
        verify(headerPresenter).onPause(fragment);
    }

    @Test
    public void shouldRedirectOnDestroyViewToHeaderPresenter() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);
        headerPresenter.onDestroyView(fragment);
    }

    @Test
    public void shouldRegisterOnItemClickHandlerWithList() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(listView).setOnItemClickListener(presenter);
    }

    @Test
    public void shouldPlayLikedTracksOnListItemClick() {
        final PropertySet clickedTrack = TestPropertySets.expectedLikedTrackForLikesScreen();
        final List<Urn> likedUrns = Arrays.asList(TRACK_URN);
        final Observable<List<Urn>> likedUrnsObservable = Observable.just(likedUrns);
        when(listView.getItemAtPosition(0)).thenReturn(clickedTrack);
        when(playbackOperations.playLikes(eq(clickedTrack.get(TrackProperty.URN)), eq(0), isA(PlaySessionSource.class)))
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

    @Test
    public void shouldListenForDownloadStopEventAndUpdateTheListToRemoveDownlaodIndicators() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.stop());
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldPrependTrackOnLikedEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(track));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK_URN, true, 5));

        verify(adapter).prependItem(track);
    }

    @Test
    public void shouldRemoveTrackOnUnlikedEvent() {
        PropertySet item = PropertySet.from(EntityProperty.URN.bind(TRACK_URN));
        when(adapter.getCount()).thenReturn(1);
        when(adapter.getItem(0)).thenReturn(item);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK_URN, false, 5));

        verify(adapter).removeAt(0);
    }
}