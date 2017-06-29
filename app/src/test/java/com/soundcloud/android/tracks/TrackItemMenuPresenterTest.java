package com.soundcloud.android.tracks;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent.LikeStatus;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

public class TrackItemMenuPresenterTest extends AndroidUnitTest {

    private static final String SCREEN = "screen";
    @Mock TrackItemRepository trackRepository;
    @Mock LikeOperations likeOperations;
    @Mock SharePresenter sharePresenter;
    @Mock RepostOperations repostOperations;
    @Mock PlaylistOperations playlistOperations;
    @Mock ScreenProvider screenProvider;
    @Mock PlayQueueManager playQueueManager;
    @Mock PlaybackInitiator playbackInitiator;
    @Mock PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock StartStationHandler stationHandler;
    @Mock Context context;
    @Mock FragmentActivity activity;
    @Mock AccountOperations accountOperations;
    @Mock EventTracker tracker;
    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    @Mock ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;
    @Mock FeedbackController feedbackController;
    @Mock NavigationExecutor navigationExecutor;

    @Mock DelayedLoadingDialogPresenter.Builder dialogBuilder;
    @Mock PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock PopupMenuWrapper popupMenuWrapper;
    @Mock MenuItem menuItem;
    @Mock View view;

    @Captor ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private final TestEventBus eventBus = new TestEventBus();
    private Track track = ModelFixtures.track();
    private TrackItem trackItem = ModelFixtures.trackItem(track);

    private TrackItemMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(view.getContext()).thenReturn(context());
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        when(trackRepository.track(any(Urn.class))).thenReturn(Maybe.empty());
        when(screenProvider.getLastScreenTag()).thenReturn(SCREEN);
        when(playbackInitiator.playTracks(Matchers.anyListOf(Urn.class), eq(0), any(PlaySessionSource.class)))
                .thenReturn(Single.never());

        presenter = new TrackItemMenuPresenter(popupMenuWrapperFactory,
                                               trackRepository,
                                               eventBus,
                                               context,
                                               likeOperations,
                                               repostOperations,
                                               playlistOperations,
                                               screenProvider,
                                               sharePresenter,
                                               stationHandler,
                                               accountOperations,
                                               playQueueManager,
                                               playbackInitiator,
                                               playbackFeedbackHelper,
                                               tracker,
                                               changeLikeToSaveExperiment,
                                               changeLikeToSaveExperimentStringHelper,
                                               feedbackController,
                                               navigationExecutor);
    }

    @Test
    public void clickingOnAddToLikesAddTrackLike() {
        final SingleSubject<LikeOperations.LikeResult> likeObservable = SingleSubject.create();
        when(likeOperations.toggleLike(trackItem.getUrn(), !trackItem.isUserLike())).thenReturn(likeObservable);
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(likeObservable.hasObservers()).isTrue();
    }

    @Test
    public void shouldGetLikeActionTitle() {
        when(trackRepository.track(any(Urn.class))).thenReturn(Maybe.just(trackItem.updatedWithLike(LikeStatus.create(trackItem.getUrn(), false))));

        presenter.show(activity, view, trackItem, 0);

        verify(changeLikeToSaveExperimentStringHelper).getString(ExperimentString.LIKE);
        verify(popupMenuWrapper).setItemVisible(R.id.add_to_likes, true);
    }

    @Test
    public void shouldGetUnlikeActionTitle() {
        when(trackRepository.track(any(Urn.class))).thenReturn(Maybe.just(trackItem.updatedWithLike(LikeStatus.create(trackItem.getUrn(), true))));

        presenter.show(activity, view, trackItem, 0);

        verify(changeLikeToSaveExperimentStringHelper).getString(ExperimentString.UNLIKE);
        verify(popupMenuWrapper).setItemVisible(R.id.add_to_likes, true);
    }

    @Test
    public void clickRepostItemRepostsTrack() {
        final SingleSubject<RepostOperations.RepostResult> repostObservable = SingleSubject.create();
        when(repostOperations.toggleRepost(trackItem.getUrn(), !trackItem.isUserRepost())).thenReturn(repostObservable);
        when(menuItem.getItemId()).thenReturn(R.id.toggle_repost);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(repostObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickingOnShareItemSharesTrack() {
        when(menuItem.getItemId()).thenReturn(R.id.share);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        EventContextMetadata eventContextMetadata =
                EventContextMetadata.builder()
                                    .pageName(screenProvider.getLastScreenTag())
                                    .pageUrn(Urn.NOT_SET)
                                    .isFromOverflow(true)
                                    .build();
        verify(sharePresenter).share(context, trackItem, eventContextMetadata, null);
    }

    @Test
    public void clickingOnPlayNextInsertsNextWhenQueueIsNotEmpty() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        verify(playQueueManager).insertNext(trackItem.getUrn());
    }

    @Test
    public void clickIgnoredWhileShowing() {
        presenter.show(activity, view, trackItem, 0);
        presenter.show(activity, view, trackItem, 0);

        verify(popupMenuWrapper, times(1)).show();
    }

    @Test
    public void clickAfterDismissalShowsPopup() {
        presenter.show(activity, view, trackItem, 0);
        presenter.onDismiss();
        presenter.show(activity, view, trackItem, 0);

        verify(popupMenuWrapper, times(2)).show();
    }

    @Test
    public void clickingOnPlayNextStartsPlaybackWhenQueueIsEmpty() {
        final PlaySessionSource playSessionSource = PlaySessionSource.forPlayNext(screenProvider.getLastScreenTag());
        when(menuItem.getItemId()).thenReturn(R.id.play_next);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        verify(playbackInitiator).playTracks(singletonList(trackItem.getUrn()), 0, playSessionSource);
        verify(playQueueManager, never()).insertNext(trackItem.getUrn());
    }

    @Test
    public void clickOnStationShouldProxyToStationHandler() {
        when(menuItem.getItemId()).thenReturn(R.id.start_station);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        verify(stationHandler).openStationWithSeedTrack(eq(activity), eq(trackItem.getUrn()), uiEventArgumentCaptor.capture());
        assertThat(uiEventArgumentCaptor.getValue().kind()).isEqualTo(UIEvent.Kind.NAVIGATION);
        assertThat(uiEventArgumentCaptor.getValue().clickObjectUrn().get()).isEqualTo(trackItem.getUrn());
    }

    @Test
    public void playNextIsDisabledWhenTrackIsBlocked() throws Exception {
        final TrackItem blockedTrackItem = ModelFixtures.trackItem(track.toBuilder().blocked(true).build());

        presenter.show(activity, view, blockedTrackItem, 0);

        verify(popupMenuWrapper).setItemEnabled(R.id.play_next, false);
    }

    @Test
    public void clickingOnPlayNextPublishesTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        final UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.PLAY_NEXT);
        assertThat(event.clickObjectUrn().get()).isEqualTo(trackItem.getUrn());
        assertThat(event.originScreen().get()).isEqualTo(SCREEN);
    }
}
