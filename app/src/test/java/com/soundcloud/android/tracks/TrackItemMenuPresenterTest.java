package com.soundcloud.android.tracks;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.PlayQueueConfiguration;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

public class TrackItemMenuPresenterTest extends AndroidUnitTest {

    private static final String SCREEN = "screen";
    @Mock TrackRepository trackRepository;
    @Mock LikeOperations likeOperations;
    @Mock ShareOperations shareOperations;
    @Mock RepostOperations repostOperations;
    @Mock PlaylistOperations playlistOperations;
    @Mock ScreenProvider screenProvider;
    @Mock PlayQueueManager playQueueManager;
    @Mock PlaybackInitiator playbackInitiator;
    @Mock PlaybackToastHelper playbackToastHelper;
    @Mock PlayQueueConfiguration playQueueConfiguration;
    @Mock StartStationHandler stationHandler;
    @Mock Context context;
    @Mock FragmentActivity activity;
    @Mock AccountOperations accountOperations;
    @Mock EventTracker tracker;

    @Mock DelayedLoadingDialogPresenter.Builder dialogBuilder;
    @Mock PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock PopupMenuWrapper popupMenuWrapper;
    @Mock MenuItem menuItem;
    @Mock View view;
    @Captor ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private final TestEventBus eventBus = new TestEventBus();
    private TrackItem trackItem = createTrackItem();

    private TrackItemMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(view.getContext()).thenReturn(context());
        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(popupMenuWrapper.findItem(anyInt())).thenReturn(menuItem);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());
        when(screenProvider.getLastScreenTag()).thenReturn(SCREEN);
        when(playQueueConfiguration.isEnabled()).thenReturn(true);
        when(playbackInitiator.playTracks(Matchers.anyListOf(Urn.class), eq(0), any(PlaySessionSource.class)))
                .thenReturn(Observable.<PlaybackResult>empty());

        presenter = new TrackItemMenuPresenter(popupMenuWrapperFactory,
                                               trackRepository,
                                               eventBus,
                                               context,
                                               likeOperations,
                                               repostOperations,
                                               playlistOperations,
                                               screenProvider,
                                               shareOperations,
                                               stationHandler,
                                               accountOperations,
                                               playQueueConfiguration,
                                               playQueueManager,
                                               playbackInitiator,
                                               playbackToastHelper,
                                               tracker);
    }

    @Test
    public void clickingOnAddToLikesAddTrackLike() {
        final PublishSubject<PropertySet> likeObservable = PublishSubject.create();
        when(likeOperations.toggleLike(trackItem.getUrn(), !trackItem.isLiked())).thenReturn(likeObservable);
        when(menuItem.getItemId()).thenReturn(R.id.add_to_likes);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        assertThat(likeObservable.hasObservers()).isTrue();
    }

    @Test
    public void clickRepostItemRepostsTrack() {
        final PublishSubject<PropertySet> repostObservable = PublishSubject.create();
        when(repostOperations.toggleRepost(trackItem.getUrn(), !trackItem.isRepostedByCurrentUser())).thenReturn(repostObservable);
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
                                    .invokerScreen(ScreenElement.LIST.get())
                                    .contextScreen(screenProvider.getLastScreenTag())
                                    .pageName(screenProvider.getLastScreenTag())
                                    .pageUrn(Urn.NOT_SET)
                                    .isFromOverflow(true)
                                    .build();
        verify(shareOperations).share(context, trackItem.getSource(), eventContextMetadata, null);
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

        verify(stationHandler).openStationWithSeedTrack(any(Context.class), eq(trackItem.getUrn()), uiEventArgumentCaptor.capture());
        assertThat(uiEventArgumentCaptor.getValue().getKind()).isEqualTo(UIEvent.KIND_NAVIGATION);
        assertThat(uiEventArgumentCaptor.getValue().get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN)).isEqualTo(trackItem.getUrn().toString());
    }

    @Test
    public void playNextIsDisabledWhenTrackIsBlocked() throws Exception {
        trackItem.update(PropertySet.from(TrackProperty.BLOCKED.bind(true)));

        presenter.show(activity, view, trackItem, 0);

        verify(popupMenuWrapper).setItemEnabled(R.id.play_next, false);
    }

    @Test
    public void clickingOnPlayNextPublishesTrackingEvent() {
        when(menuItem.getItemId()).thenReturn(R.id.play_next);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_PLAY_NEXT);
        assertThat(event.get(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN)).isEqualTo(trackItem.getUrn().toString());
        assertThat(event.get(PlayableTrackingKeys.KEY_ORIGIN_SCREEN)).isEqualTo(SCREEN);
    }

    private TrackItem createTrackItem() {
        return TrackItem.from(ModelFixtures.create(ApiTrack.class));
    }
}
