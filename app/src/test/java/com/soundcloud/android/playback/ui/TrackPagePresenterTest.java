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
import com.soundcloud.android.cast.CastPlayerStripController;
import com.soundcloud.android.cast.CastPlayerStripControllerFactory;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlayerUpsellCopyExperiment;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.PlayerUpsellImpressionController;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.ui.view.PlayerStripView;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.PlayerUpsellView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.android.waveform.WaveformOperations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import android.support.v4.app.FragmentActivity;
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
    @Mock private EmptyViewControllerFactory emptyControllerFactory;
    @Mock private ErrorViewController errorViewController;
    @Mock private EmptyViewController emptyViewController;
    @Mock private SkipListener skipListener;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private TrackPageMenuController.Factory trackMenuControllerFactory;
    @Mock private TrackPageMenuController trackPageMenuController;
    @Mock private PlaybackProgress playbackProgress;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerUpsellImpressionController upsellImpressionController;
    @Mock private LikeButtonPresenter likeButtonPresenter;
    @Mock private IntroductoryOverlayPresenter introductoryOverlayPresenter;
    @Mock private PlayerUpsellCopyExperiment upsellCopyExperiment;
    @Mock private CastPlayerStripControllerFactory castPlayerStripControllerFactory;
    @Mock private CastPlayerStripController castPlayerStripController;

    @Captor private ArgumentCaptor<PlaybackProgress> progressArgumentCaptor;

    private TrackQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));

    private TrackPagePresenter presenter;
    private View trackView;
    private TestDateProvider dateProvider;
    private FragmentActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = activity();
        ViewGroup container = new FrameLayout(activity);
        presenter = new TrackPagePresenter(waveformOperations,
                                           featureOperations,
                                           listener,
                                           likeButtonPresenter,
                                           introductoryOverlayPresenter,
                                           waveformFactory,
                                           artworkFactory,
                                           playerOverlayControllerFactory,
                                           trackMenuControllerFactory,
                                           castPlayerStripControllerFactory,
                                           adOverlayControllerFactory,
                                           errorControllerFactory,
                                           emptyControllerFactory,
                                           castConnectionHelper,
                                           resources(),
                                           upsellImpressionController,
                                           upsellCopyExperiment);
        when(waveformFactory.create(any(WaveformView.class))).thenReturn(waveformViewController);
        when(artworkFactory.create(any(PlayerTrackArtworkView.class))).thenReturn(artworkController);
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(playerOverlayController);
        when(trackMenuControllerFactory.create(any(View.class))).thenReturn(trackPageMenuController);
        when(adOverlayControllerFactory.create(any(View.class), any(AdOverlayListener.class))).thenReturn(
                adOverlayController);
        when(castPlayerStripControllerFactory.create(any(PlayerStripView.class), any(PlayerUpsellView.class))).thenReturn(castPlayerStripController);
        when(errorControllerFactory.create(any(View.class))).thenReturn(errorViewController);
        when(emptyControllerFactory.create(any(View.class))).thenReturn(emptyViewController);
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
                                                 R.drawable.ic_player_liked, R.drawable.ic_player_like);
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).footerPlayToggle).isChecked();
    }

    @Test
    public void clearItemViewHidesSnippedAndUpsell() {
        presenter.clearItemView(trackView);

        assertThat(getHolder(trackView).upsellView).isGone();
    }

    @Test
    public void bindItemViewLoadsStationsContext() {
        final PlayerTrackState trackState = new PlayerTrackState(PlayableFixtures.expectedTrackForPlayer(),
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

        verify(waveformViewController).showPlayingState(eq(TestPlaybackProgress.getPlaybackProgress(10,
                                                                                                    20,
                                                                                                    dateProvider)));
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
        populateTrackPage();
        getHolder(trackView).likeToggle.setEnabled(false); // Toggle disable whilst updating
        final LikesStatusEvent.LikeStatus likeStatus = LikesStatusEvent.LikeStatus.create(TRACK_URN, true, 1);

        presenter.onPlayableLiked(trackView, likeStatus);

        assertThat(getHolder(trackView).likeToggle).isChecked();
    }

    @Test
    public void updateAssociationsWithLikedCountPropertyUpdatesLikeCountBelow10k() {
        populateTrackPage();
        final LikesStatusEvent.LikeStatus likeStatus = LikesStatusEvent.LikeStatus.create(TRACK_URN, true, 9999);

        presenter.onPlayableLiked(trackView, likeStatus);

        verify(likeButtonPresenter).setLikeCount(getHolder(trackView).likeToggle, 9999,
                                                 R.drawable.ic_player_liked, R.drawable.ic_player_like);
    }

    @Test
    public void doNotDisplayUnknownLikeCounts() {
        final LikesStatusEvent.LikeStatus likeStatus = LikesStatusEvent.LikeStatus.create(TRACK_URN, true, -1);

        presenter.onPlayableLiked(trackView, likeStatus);

        assertThat(getHolder(trackView).likeToggle).hasText("");
    }

    @Test
    public void updateAssociationsWithRepostedPropertyUpdatesRepostStatusOnMenuController() throws Exception {
        populateTrackPage();
        final RepostStatus repostStatus = RepostStatus.createReposted(TRACK_URN);

        presenter.onPlayableReposted(trackView, repostStatus);

        verify(trackPageMenuController).setIsUserRepost(true);
    }

    @Test
    public void showToastWhenUserRepostedATrack() {
        populateTrackPage();
        final RepostStatus repostStatus = RepostStatus.createReposted(TRACK_URN);

        presenter.onPlayableReposted(trackView, repostStatus);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(RuntimeEnvironment.application.getString(R.string.reposted_to_followers));
    }

    @Test
    public void showToastWhenUserUnpostedATrack() {
        populateTrackPage();
        final RepostStatus repostStatus = RepostStatus.createUnposted(TRACK_URN);

        presenter.onPlayableReposted(trackView, repostStatus);

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

        verify(trackPageMenuController).handleShare(activity);
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
    public void onForegroundSubscribesCastController() throws Exception {
        populateTrackPage();

        presenter.onForeground(trackView);

        verify(castPlayerStripController).subscribeToEvents();
    }

    @Test
    public void onBackgroundUnsubscribesCastController() throws Exception {
        populateTrackPage();

        presenter.onBackground(trackView);

        verify(castPlayerStripController).unsubscribeFromEvents();
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

        assertThat(getHolder(trackView).upsellView).isGone();
    }

    @Test
    public void bindingSnippedTrackInHighTierShowsUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        bindSnippedTrack();

        assertThat(getHolder(trackView).upsellView).isVisible();
    }

    @Test
    public void bindingUpsellableHighTierTrackWithoutUpsellFeatureHidesUpsellIcon() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        bindUpsellableHighTierTrack();

        assertThat(getHolder(trackView).upsellView).isGone();
    }

    @Test
    public void bindingUpsellableHighTierTrackWhileAllowingUpsellFeatureShowsUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        final TrackItem track = bindUpsellableHighTierTrack();

        final TrackPageHolder holder = getHolder(trackView);
        assertThat(holder.upsellView.getUpsellButton()).isVisible();
        assertThat(holder.upsellView.getUpsellButton().getTag()).isEqualTo(track.getUrn());
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
    public void shouldShowShareButtonWhenNotCasting() {
        when(castConnectionHelper.getDeviceName()).thenReturn(null);

        populateTrackPage();

        assertThat(getHolder(trackView).shareButton).isVisible();
    }

    @Test
    public void bindingHighTierTrackSetsExperimentalCopyOnUpsellCta() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        when(upsellCopyExperiment.getUpsellCtaId()).thenReturn(R.string.playback_upsell_2);

        bindUpsellableHighTierTrack();

        assertThat(getHolder(trackView).upsellView.getUpsellText().getText())
                .isEqualTo(resources().getText(R.string.playback_upsell_2));
    }

    @Test
    public void showingPlayQueueIntroductoryOverlayForwardsCallToPresenterWithCorrectParameters() {
        presenter.showIntroductoryOverlayForPlayQueue(trackView);

        verify(introductoryOverlayPresenter).showIfNeeded(IntroductoryOverlayKey.PLAY_QUEUE,
                                                          getHolder(trackView).playQueueButton,
                                                          resources().getString(R.string.play_queue_introductory_overlay_title),
                                                          resources().getString(R.string.play_queue_introductory_overlay_description));
    }

    @Test
    public void shouldNotShowPlayQueueButtonWhenCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        assertThat(getHolder(trackView).playQueueButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindingEmptyTrackShowsTheEmptyViewController() {
        bindEmptyTrack();

        verify(emptyViewController).show();
    }

    private TrackPageHolder getHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void populateTrackPage() {
        final TrackItem source = ModelFixtures.trackItem(PlayableFixtures.expectedTrackBuilderForPlayer().snipped(true).build());
        presenter.bindItemView(trackView, new PlayerTrackState(source, true, true, viewVisibilityProvider));
    }

    private void bindSnippedTrack() {
        final TrackItem snippedTrack = PlayableFixtures.upsellableTrack();
        presenter.bindItemView(trackView, new PlayerTrackState(snippedTrack, true, true, viewVisibilityProvider));
    }

    private TrackItem bindUpsellableHighTierTrack() {
        final TrackItem source = PlayableFixtures.upsellableTrackForPlayer();
        presenter.bindItemView(trackView, new PlayerTrackState(source, true, true, viewVisibilityProvider));
        return source;
    }

    private void bindEmptyTrack() {
        presenter.bindItemView(trackView, new PlayerTrackState(true, true, viewVisibilityProvider));
    }
}
