package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    private static final Urn TRACK_URN = Urn.forTrack(123);

    private TrackLikesPresenter presenter;

    @Mock private TrackLikeOperations likeOperations;
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
        when(listView.getHeaderViewsCount()).thenReturn(1);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(likeOperations.likedTracks()).thenReturn(likedTracksObservable);
        when(likeOperations.onTrackLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onTrackUnliked()).thenReturn(Observable.<Urn>empty());
    }

    @Test
    public void shouldOnSubscribeListObserversToHeaderPresenter() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(headerPresenter).onSubscribeListObservers(any(ListBinding.class));
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
        PlaybackResult playbackResult = PlaybackResult.success();
        final TrackItem clickedTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(playbackOperations.playLikes(eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(playbackResult));

        presenter.onItemClick(listView, view, 1, 0);

        testSubscriber.assertReceivedOnNext(Arrays.asList(playbackResult));
    }

    @Test
    public void shouldNotPlayTracksOnListItemClickIfItemIsNull() {
        when(listView.getItemAtPosition(0)).thenReturn(null);
        presenter.onItemClick(listView, view, 0, 0);
        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

}