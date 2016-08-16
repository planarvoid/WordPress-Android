package com.soundcloud.android.tracks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.TheTracker;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import java.util.Collections;

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
    @Mock FeatureFlags featureFlags;
    @Mock StartStationHandler stationHandler;
    @Mock Context context;
    @Mock FragmentActivity activity;
    @Mock AccountOperations accountOperations;
    @Mock TheTracker tracker;

    @Mock DelayedLoadingDialogPresenter.Builder dialogBuilder;
    @Mock PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock PopupMenuWrapper popupMenuWrapper;
    @Mock MenuItem menuItem;
    @Mock View view;

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
        when(featureFlags.isEnabled(Flag.PLAY_QUEUE)).thenReturn(true);
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
                                               featureFlags,
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
        when(repostOperations.toggleRepost(trackItem.getUrn(), !trackItem.isReposted())).thenReturn(repostObservable);
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
        when(menuItem.getItemId()).thenReturn(R.id.play_next);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        presenter.show(activity, view, trackItem, 0);
        presenter.onMenuItemClick(menuItem, context);

        verify(playbackInitiator).playTracks(eq(Collections.singletonList(trackItem.getUrn())),
                                             eq(0), any(PlaySessionSource.class));
        verify(playQueueManager, never()).insertNext(trackItem.getUrn());
    }

    @Test
    public void playNextIsDisabledWhenTrackIsBlocked() throws Exception {
        trackItem.update(PropertySet.from(TrackProperty.BLOCKED.bind(true)));

        presenter.show(activity, view, trackItem, 0);

        verify(popupMenuWrapper).setItemEnabled(R.id.play_next, false);
    }

    @Test
    public void shouldBuildEventContextMetadataUsingTrackSourceInfoIfPresent() {
        final String originScreen = "trackSourceInfoScreen";
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(originScreen, false);

        presenter.show(activity, view, trackItem, 0, Optional.of(trackSourceInfo));

        assertThat(presenter.getEventContextMetadata().contextScreen()).isEqualTo(originScreen);
        assertThat(presenter.getEventContextMetadata().pageName()).isEqualTo(originScreen);
        assertThat(presenter.getEventContextMetadata().trackSourceInfo()).isEqualTo(trackSourceInfo);
    }

    @Test
    public void shouldBuildEventContextMetadataUsingScreenProviderIfTrackSourceInfoIsAbsent() {
        presenter.show(activity, view, trackItem, 0);

        assertThat(presenter.getEventContextMetadata().contextScreen()).isEqualTo(SCREEN);
        assertThat(presenter.getEventContextMetadata().pageName()).isEqualTo(SCREEN);
        assertThat(presenter.getEventContextMetadata().trackSourceInfo()).isNull();
    }

    private TrackItem createTrackItem() {
        return TrackItem.from(ModelFixtures.create(ApiTrack.class));
    }
}
