package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.ads.AdOverlayController.AdOverlayListener;
import com.soundcloud.android.ads.AdOverlayControllerFactory;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlayerUpsellCopyExperiment;
import com.soundcloud.android.configuration.experiments.ShareAsTextButtonExperiment;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.PlayerUpsellImpressionController;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TrackPagePresenterTest extends AndroidUnitTest {

    private static final int PLAY_DURATION = 20000;
    private static final int FULL_DURATION = 30000;
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private WaveformOperations waveformOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private TrackPageListener listener;
    @Mock private WaveformViewController.Factory waveformFactory;
    @Mock private WaveformViewController waveformViewController;
    @Mock private PlayerArtworkController.Factory artworkFactory;
    @Mock private PlayerArtworkController artworkController;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdOverlayControllerFactory adOverlayControllerFactory;
    @Mock private AdOverlayController adOverlayController;
    @Mock private ErrorViewControllerFactory errorControllerFactory;
    @Mock private ErrorViewController errorViewController;
    @Mock private SkipListener skipListener;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private TrackPageMenuController.Factory trackMenuControllerFactory;
    @Mock private TrackPageMenuController trackPageMenuController;
    @Mock private PlaybackProgress playbackProgress;
    @Mock private ImageOperations imageOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlayerUpsellImpressionController upsellImpressionController;
    @Mock private LikeButtonPresenter likeButtonPresenter;
    @Mock private ShareAsTextButtonExperiment shareExperiment;
    @Mock private PlayerUpsellCopyExperiment upsellCopyExperiment;

    @Captor private ArgumentCaptor<PlaybackProgress> progressArgumentCaptor;

    private TrackQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));

    private TrackPagePresenter presenter;
    private View trackView;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        ViewGroup container = new FrameLayout(context());
        presenter = new TrackPagePresenter(waveformOperations,
                                           featureOperations,
                                           listener,
                                           likeButtonPresenter,
                                           waveformFactory,
                                           artworkFactory,
                                           playerOverlayControllerFactory,
                                           trackMenuControllerFactory,
                                           adOverlayControllerFactory,
                                           errorControllerFactory,
                                           castConnectionHelper,
                                           resources(),
                                           upsellImpressionController,
                                           shareExperiment,
                                           upsellCopyExperiment,
                                           featureFlags);
        when(waveformFactory.create(any(WaveformView.class))).thenReturn(waveformViewController);
        when(artworkFactory.create(any(PlayerTrackArtworkView.class))).thenReturn(artworkController);
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(playerOverlayController);
        when(trackMenuControllerFactory.create(any(View.class))).thenReturn(trackPageMenuController);
        when(adOverlayControllerFactory.create(any(View.class), any(AdOverlayListener.class))).thenReturn(
                adOverlayController);
        when(errorControllerFactory.create(any(View.class))).thenReturn(errorViewController);
        when(upsellCopyExperiment.getUpsellCtaId()).thenReturn(R.string.playback_upsell_1);
        trackView = presenter.createItemView(container, skipListener);
        dateProvider = new TestDateProvider();
    }

    @Test
    public void bindItemViewSetsDurationOnWaveformController() {
        populateTrackPage();
        verify(waveformViewController).setDurations(PLAY_DURATION, FULL_DURATION);
    }

    @Test
    public void bindItemViewSetsUrnOnErrorViewController() {
        populateTrackPage();
        verify(errorViewController).setUrn(TRACK_URN);
    }

    @Test
    public void bindItemViewSetsInitialLikeStatesFromTrackData() {
        populateTrackPage();
        assertThat(getHolder(trackView).likeToggle).isChecked();
        verify(likeButtonPresenter).setLikeCount(getHolder(trackView).likeToggle, 1,
                                                 R.drawable.player_like_active, R.drawable.player_like);
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).footerPlayToggle).isChecked();
    }

    @Test
    public void clearItemViewHidesSnippedAndUpsell() {
        presenter.clearItemView(trackView);

        assertThat(getHolder(trackView).upsellContainer).isGone();
    }

    @Test
    public void bindItemViewLoadsStationsContext() {
        final PlayerTrackState trackState = new PlayerTrackState(TestPropertySets.expectedTrackForPlayer(),
                                                                 true,
                                                                 true,
                                                                 viewVisibilityProvider);
        final StationRecord station = StationFixtures.getStation(Urn.forTrackStation(123L));
        trackState.setStation(station);

        presenter.bindItemView(trackView, trackState);

        assertThat(getHolder(trackView).trackContext).isVisible();
    }

    @Test
    public void bindItemViewClearsStationsContextIfTrackIsNotFromStation() {
        populateTrackPage();

        assertThat(getHolder(trackView).trackContext).isNotVisible();
    }

    @Test
    public void playingStateShowsBackgroundOnTitle() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).title.isShowingBackground()).isTrue();
    }

    @Test
    public void playingStateShowsBackgroundOnUser() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).user.isShowingBackground()).isTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        assertThat(getHolder(trackView).footerPlayToggle).isNotChecked();
    }

    @Test
    public void pausedStateHidesBackgroundOnTitle() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        assertThat(getHolder(trackView).title.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateHidesBackgroundOnUser() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        assertThat(getHolder(trackView).user.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateShowsBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).timestamp.isShowingBackground()).isTrue();
    }

    @Test
    public void pauseStateHidesBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        assertThat(getHolder(trackView).timestamp.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20, dateProvider), true, true);

        verify(waveformViewController).showPlayingState(eq(TestPlaybackProgress.getPlaybackProgress(10, 20, dateProvider)));
    }

    @Test
    public void playingStateWithCurrentTrackDoesNotResetProgress() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), true, true);

        verify(waveformViewController, never()).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void playingStateWithCurrentTrackSetsPlayStateOnArtwork() {
        final PlayStateEvent state = TestPlayStates.playing(10, 20, dateProvider);
        presenter.setPlayState(trackView, state, true, true);

        verify(artworkController).setPlayState(state, true);
    }

    @Test
    public void playingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackClearsProgress() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), false, true);

        verify(waveformViewController).clearProgress();
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsBufferingStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), true, true);
        verify(waveformViewController).showBufferingState();
    }

    @Test
    public void bufferingStateWithOtherTrackClearsProgress() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), false, true);

        verify(waveformViewController).clearProgress();
    }

    @Test
    public void bufferingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void setExpandedShouldHideFooterControl() {
        presenter.setExpanded(trackView, playQueueItem, true);
        assertThat(getHolder(trackView).footer).isGone();
    }

    @Test
    public void setCollapsedShouldShowFooterControl() {
        presenter.setCollapsed(trackView);
        assertThat(getHolder(trackView).footer).isVisible();
    }

    @Test
    public void onPlayerSlidePassesSlideValueToWaveformController() {
        presenter.onPlayerSlide(trackView, 1);
        verify(waveformViewController).onPlayerSlide(1);
    }

    @Test
    public void createTrackPageSetsArtworkAsScrubListenerOnWaveformController() {
        verify(waveformViewController).addScrubListener(artworkController);
    }

    @Test
    public void setProgressSetsProgressOnWaveformController() {
        presenter.setProgress(trackView, playbackProgress);
        verify(waveformViewController).setProgress(playbackProgress);
    }

    @Test
    public void setProgressSetsProgressOnArtworkController() {
        presenter.setProgress(trackView, playbackProgress);
        verify(artworkController).setProgress(playbackProgress);
    }

    @Test
    public void setProgressSetsProgressOnMenuController() {
        presenter.setProgress(trackView, playbackProgress);
        verify(trackPageMenuController).setProgress(playbackProgress);
    }

    @Test
    public void setProgressWithEmptyProgressDoesNotSetProgressOnWaveformController() {
        presenter.setProgress(trackView, PlaybackProgress.empty());
        verify(waveformViewController, never()).setProgress(playbackProgress);
    }

    @Test
    public void setProgressWithEmptyProgressDoesNotSetProgressOnArtworkController() {
        presenter.setProgress(trackView, PlaybackProgress.empty());
        verify(artworkController, never()).setProgress(playbackProgress);
    }

    @Test
    public void setProgressWithEmptyProgressDoesNotSetProgressOnMenuController() {
        presenter.setProgress(trackView, PlaybackProgress.empty());
        verify(trackPageMenuController, never()).setProgress(playbackProgress);
    }

    @Test
    public void updateAssociationsWithLikedPropertyUpdatesLikeToggle() {
        getHolder(trackView).likeToggle.setEnabled(false); // Toggle disable whilst updating
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromLike(TRACK_URN, true, 1);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        assertThat(getHolder(trackView).likeToggle).isChecked();
    }

    @Test
    public void updateAssociationsWithLikedCountPropertyUpdatesLikeCountBelow10k() {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromLike(TRACK_URN, true, 9999);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        verify(likeButtonPresenter).setLikeCount(getHolder(trackView).likeToggle, 9999,
                                                 R.drawable.player_like_active, R.drawable.player_like);
    }

    @Test
    public void doNotDisplayUnknownLikeCounts() {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromLike(TRACK_URN, true, -1);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        assertThat(getHolder(trackView).likeToggle).hasText("");
    }

    @Test
    public void updateAssociationsWithRepostedPropertyUpdatesRepostStatusOnMenuController() throws Exception {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromRepost(TRACK_URN, true);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        verify(trackPageMenuController).setIsUserRepost(true);
    }

    @Test
    public void showToastWhenUserRepostedATrack() {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromRepost(TRACK_URN, true);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(RuntimeEnvironment.application.getString(R.string.reposted_to_followers));
    }

    @Test
    public void showToastWhenUserUnpostedATrack() {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromRepost(TRACK_URN, false);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(RuntimeEnvironment.application.getString(R.string.unposted_to_followers));
    }

    @Test
    public void toggleLikeOnTrackCallsListenerWithLikeStatus() {
        populateTrackPage();

        getHolder(trackView).likeToggle.performClick();

        verify(listener).onToggleLike(false, TRACK_URN);
    }

    @Test
    public void clickShareOnTrackCallsListenerWithShare() {
        populateTrackPage();

        getHolder(trackView).shareButton.performClick();

        verify(trackPageMenuController).handleShare(context());
    }

    @Test
    public void clickUsernameCallsListenerOnClickUsernameWithActivityContext() {
        populateTrackPage();

        final View user = getHolder(trackView).profileLink;
        user.performClick();

        verify(listener).onGotoUser(user.getContext(), Urn.forUser(456L));
    }

    @Test
    public void togglePlayOnFooterTogglePlayClick() {
        populateTrackPage();

        getHolder(trackView).footerPlayToggle.performClick();

        verify(listener).onFooterTogglePlay();
    }

    @Test
    public void togglePlayOnTrackPageArtworkClick() {
        populateTrackPage();

        getHolder(trackView).artworkView.performClick();

        verify(listener).onTogglePlay();
    }

    @Test
    public void nextOnTrackPagerNextClick() {
        populateTrackPage();

        getHolder(trackView).nextButton.performClick();

        verify(skipListener).onNext();
    }

    @Test
    public void nextOnTrackPagerPreviousClick() {
        populateTrackPage();

        getHolder(trackView).previousButton.performClick();

        verify(skipListener).onPrevious();
    }

    @Test
    public void footerTapOnFooterControlsClick() {
        populateTrackPage();

        getHolder(trackView).footer.performClick();

        verify(listener).onFooterTap();
    }

    @Test
    public void playerCloseOnPlayerCloseIndicatorClick() {
        populateTrackPage();

        getHolder(trackView).closeIndicator.performClick();

        verify(listener).onPlayerClose();
    }

    @Test
    public void playerCloseOnPlayerBottomCloseClick() {
        populateTrackPage();

        getHolder(trackView).bottomClose.performClick();

        verify(listener).onPlayerClose();
    }

    @Test
    public void onPageChangeCallsDismissOnMenuController() throws Exception {
        populateTrackPage();

        presenter.onPageChange(trackView);

        verify(trackPageMenuController).dismiss();
    }

    @Test
    public void onPositionSetHidesPreviousButtonForFirstTrack() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 0, 5);

        assertThat(getHolder(trackView).nextButton).isVisible();
        assertThat(getHolder(trackView).previousButton).isInvisible();
    }

    @Test
    public void onPositionSetHidesNextButtonForLastTrack() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 4, 5);

        assertThat(getHolder(trackView).nextButton).isInvisible();
        assertThat(getHolder(trackView).previousButton).isVisible();
    }

    @Test
    public void onPositionSetShowsBothNavigationButtonsForTrackInMiddleOfQueue() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 2, 5);

        assertThat(getHolder(trackView).nextButton).isVisible();
        assertThat(getHolder(trackView).previousButton).isVisible();
    }

    @Test
    public void onPositionSetHidesBothNavigationButtonsForSingleTrack() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 0, 1);

        assertThat(getHolder(trackView).nextButton).isInvisible();
        assertThat(getHolder(trackView).previousButton).isInvisible();
    }

    @Test
    public void setLeaveBehindInitializesLeaveBehindController() throws Exception {
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        populateTrackPage();

        presenter.setAdOverlay(trackView, leaveBehind);

        verify(adOverlayController).initialize(leaveBehind);
    }

    @Test
    public void clearLeaveBehindClearsLeaveBehindUsingController() throws Exception {
        populateTrackPage();

        presenter.clearAdOverlay(trackView);

        verify(adOverlayController).clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        final View view = new View(RuntimeEnvironment.application);
        view.setId(R.id.toolbar_id);
        presenter.onClick(view);
    }

    @Test
    public void onClickMoreButtonCallsDismissOnLeaveBehindController() {
        populateTrackPage();
        getHolder(trackView).more.performClick();
        verify(adOverlayController).clear();
    }

    @Test
    public void onPauseDismissOnLeaveBehindController() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);

        verify(adOverlayController).clear();
    }

    @Test
    public void onPlayShowsInBackgroundDoesNotShowLeaveBehind() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, false);

        verify(adOverlayController, never()).show();
    }

    @Test
    public void onPlayShowsOnLeaveBehindController() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);

        verify(adOverlayController).show(true);
    }

    @Test
    public void onPlaybackErrorDismissOnLeaveBehindController() {
        presenter.setPlayState(trackView, TestPlayStates.error(PlayStateReason.ERROR_FAILED), true, true);

        verify(adOverlayController).clear();
    }

    @Test
    public void onPlaybackErrorShowErrorState() {
        presenter.setPlayState(trackView, TestPlayStates.error(PlayStateReason.ERROR_FAILED), true, true);

        verify(errorViewController).showError(ErrorViewController.ErrorState.FAILED);
    }

    @Test
    public void onNonErrorPlaybackEventClearAnyNonBlockedErrorState() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, false);

        verify(errorViewController).hideNonBlockedErrors();
    }

    @Test
    public void onClickMoreButtonCallsShowOnTrackMenuController() {
        populateTrackPage();
        getHolder(trackView).more.performClick();
        verify(trackPageMenuController).show();
    }

    @Test
    public void onDestroyViewCancellsControllerAnimations() {
        presenter.onDestroyView(trackView);

        verify(waveformViewController).cancelProgressAnimations();
        verify(artworkController).cancelProgressAnimations();
    }

    @Test
    public void upsellIsNotVisibleForNormalTracks() {
        populateTrackPage();

        assertThat(getHolder(trackView).upsellContainer).isGone();
    }

    @Test
    public void bindingSnippedTrackInHighTierShowsUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        bindSnippedTrack();

        assertThat(getHolder(trackView).upsellContainer).isVisible();
    }

    @Test
    public void bindingUpsellableHighTierTrackWithoutUpsellFeatureHidesUpsellIcon() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        bindUpsellableHighTierTrack();

        assertThat(getHolder(trackView).upsellContainer).isGone();
    }

    @Test
    public void bindingUpsellableHighTierTrackWhileAllowingUpsellFeatureShowsUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        final PropertySet track = bindUpsellableHighTierTrack();

        final TrackPageHolder holder = getHolder(trackView);
        assertThat(holder.upsellButton).isVisible();
        assertThat(holder.upsellButton.getTag()).isEqualTo(track.get(TrackProperty.URN));
    }

    @Test
    public void onViewSelectedWhileExpandedWithUpsellableHighTierTrackRecordsImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        bindUpsellableHighTierTrack();

        presenter.onViewSelected(trackView, playQueueItem, true);

        verify(upsellImpressionController).recordUpsellViewed(playQueueItem);
    }

    @Test
    public void onViewSelectedWhileCollapsedWithUpsellableHighTierTrackDoesNotRecordImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        bindUpsellableHighTierTrack();

        presenter.onViewSelected(trackView, playQueueItem, false);

        verify(upsellImpressionController, never()).recordUpsellViewed(any(PlayQueueItem.class));
    }

    @Test
    public void onViewSelectedWhileExpandedWithNormalTrackDoesNotRecordImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        populateTrackPage();

        presenter.onViewSelected(trackView, playQueueItem, true);

        verify(upsellImpressionController, never()).recordUpsellViewed(any(PlayQueueItem.class));
    }

    @Test
    public void setExpandedWithUpsellableHighTierTrackWhileSelectedRecordsImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        bindUpsellableHighTierTrack();

        presenter.setExpanded(trackView, playQueueItem, true);

        verify(upsellImpressionController).recordUpsellViewed(playQueueItem);
    }

    @Test
    public void setExpandedWithUpsellableTrackWhileNotSelectedDoesNotRecordImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        bindUpsellableHighTierTrack();

        presenter.setExpanded(trackView, playQueueItem, false);

        verify(upsellImpressionController, never()).recordUpsellViewed(any(PlayQueueItem.class));
    }

    @Test
    public void setExpandedWithNormalTrackWhileSelectedDoesNotRecordImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        populateTrackPage();

        presenter.setExpanded(trackView, playQueueItem, true);

        verify(upsellImpressionController, never()).recordUpsellViewed(any(PlayQueueItem.class));
    }

    @Test
    public void shouldHideShareButtonWhenCasting() {
        when(castConnectionHelper.getDeviceName()).thenReturn("chromy");

        populateTrackPage();

        assertThat(getHolder(trackView).shareButton).isGone();
        assertThat(getHolder(trackView).shareButtonText).isGone();
    }

    @Test
    public void shouldShowShareButtonWhenNotCasting() {
        when(castConnectionHelper.getDeviceName()).thenReturn(null);

        populateTrackPage();

        assertThat(getHolder(trackView).shareButton).isVisible();
        assertThat(getHolder(trackView).shareButtonText).isGone();
    }

    @Test
    public void shouldShowShareTextButtonWhenExperimentIsEnabled() {
        when(shareExperiment.showAsText()).thenReturn(true);

        populateTrackPage();

        assertThat(getHolder(trackView).shareButton).isGone();
        assertThat(getHolder(trackView).shareButtonText).isVisible();
    }

    @Test
    public void shouldHideShareTextButtonWhenExperimentIsNotEnabled() {
        when(shareExperiment.showAsText()).thenReturn(false);

        populateTrackPage();

        assertThat(getHolder(trackView).shareButton).isVisible();
        assertThat(getHolder(trackView).shareButtonText).isGone();
    }

    @Test
    public void bindingHighTierTrackSetsExperimentalCopyOnUpsellCta() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        when(upsellCopyExperiment.getUpsellCtaId()).thenReturn(R.string.playback_upsell_2);

        bindUpsellableHighTierTrack();

        assertThat(getHolder(trackView).upsellText.getText())
                .isEqualTo(resources().getText(R.string.playback_upsell_2));
    }

    private TrackPageHolder getHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void populateTrackPage() {
        final PropertySet source = TestPropertySets.expectedTrackForPlayer();
        source.put(TrackProperty.SNIPPED, true);
        presenter.bindItemView(trackView, new PlayerTrackState(source, true, true, viewVisibilityProvider));
    }

    private void bindSnippedTrack() {
        final PropertySet snippedTrack = TestPropertySets.upsellableTrack();
        presenter.bindItemView(trackView, new PlayerTrackState(snippedTrack, true, true, viewVisibilityProvider));
    }

    private PropertySet bindUpsellableHighTierTrack() {
        final PropertySet source = TestPropertySets.upsellableTrackForPlayer();
        presenter.bindItemView(trackView,
                               new PlayerTrackState(source, true, true,
                                                    viewVisibilityProvider));
        return source;
    }
}
