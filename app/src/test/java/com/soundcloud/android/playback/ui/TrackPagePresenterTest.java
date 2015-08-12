package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.Playa.Reason;
import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.ads.AdOverlayController.AdOverlayListener;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.java.collections.PropertySet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TrackPagePresenterTest extends AndroidUnitTest {

    private static final int DURATION = 20000;
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private Resources resources;
    @Mock private WaveformOperations waveformOperations;
    @Mock private TrackPageListener listener;
    @Mock private WaveformViewController.Factory waveformFactory;
    @Mock private WaveformViewController waveformViewController;
    @Mock private PlayerArtworkController.Factory artworkFactory;
    @Mock private PlayerArtworkController artworkController;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdOverlayController.Factory leaveBehindControllerFactory;
    @Mock private AdOverlayController adOverlayController;
    @Mock private ErrorViewController.Factory errorControllerFactory;
    @Mock private ErrorViewController errorViewController;
    @Mock private SkipListener skipListener;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private TrackPageMenuController.Factory trackMenuControllerFactory;
    @Mock private TrackPageMenuController trackPageMenuController;
    @Mock private PlaybackProgress playbackProgress;

    private TrackPagePresenter presenter;
    private View trackView;
    private ViewGroup container;

    @Before
    public void setUp() throws Exception {
        container = new FrameLayout(context());
        presenter = new TrackPagePresenter(waveformOperations, listener, waveformFactory,
                artworkFactory, playerOverlayControllerFactory, trackMenuControllerFactory, leaveBehindControllerFactory,
                errorControllerFactory, castConnectionHelper);
        when(waveformFactory.create(any(WaveformView.class))).thenReturn(waveformViewController);
        when(artworkFactory.create(any(PlayerTrackArtworkView.class))).thenReturn(artworkController);
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(playerOverlayController);
        when(trackMenuControllerFactory.create(any(View.class))).thenReturn(trackPageMenuController);
        when(leaveBehindControllerFactory.create(any(View.class), any(AdOverlayListener.class))).thenReturn(adOverlayController);
        when(errorControllerFactory.create(any(View.class))).thenReturn(errorViewController);
        trackView = presenter.createItemView(container, skipListener);
    }

    @Test
    public void bindItemViewSetsDurationOnWaveformController()  {
        populateTrackPage();
        verify(waveformViewController).setDuration(DURATION);
    }

    @Test
    public void bindItemViewSetsInitialLikeStatesFromTrackData() {
        populateTrackPage();
        assertThat(getHolder(trackView).likeToggle).isChecked();
        assertThat(getHolder(trackView).likeToggle).hasText("1");
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        assertThat(getHolder(trackView).footerPlayToggle).isChecked();
    }

    @Test
    public void playingStateShowsBackgroundOnTitle() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        Assertions.assertThat(getHolder(trackView).title.isShowingBackground()).isTrue();
    }

    @Test
    public void playingStateShowsBackgroundOnUser() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        Assertions.assertThat(getHolder(trackView).user.isShowingBackground()).isTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        assertThat(getHolder(trackView).footerPlayToggle).isNotChecked();
    }

    @Test
    public void pausedStateHidesBackgroundOnTitle() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        Assertions.assertThat(getHolder(trackView).title.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateHidesBackgroundOnUser() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        Assertions.assertThat(getHolder(trackView).user.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateShowsBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, true);
        Assertions.assertThat(getHolder(trackView).timestamp.isShowingBackground()).isTrue();
    }

    @Test
    public void pauseStateHidesBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        Assertions.assertThat(getHolder(trackView).timestamp.isShowingBackground()).isFalse();
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), true, true);
        verify(waveformViewController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithCurrentTrackDoesNotResetProgress() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), true, true);

        verify(waveformViewController, never()).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateWithProgressOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), true, true);
        verify(artworkController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackResetProgress() {
        presenter.setPlayState(trackView, TestPlayStates.playing(10, 20), false, true);

        verify(waveformViewController).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsBufferingStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), true, true);
        verify(waveformViewController).showBufferingState();
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsIdleStateWithProgressOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(10, 20), true, true);
        verify(artworkController).showIdleState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void bufferingStateWithOtherTrackResetProgress() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), false, true);

        verify(waveformViewController).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void bufferingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void bufferingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.buffering(), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), true, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, TestPlayStates.idle(), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void setExpandedShouldHideFooterControl() {
        presenter.setExpanded(trackView);
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

        assertThat(getHolder(trackView).likeToggle).hasText("9,999");
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

        Assertions.assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(RuntimeEnvironment.application.getString(R.string.reposted_to_followers));
    }

    @Test
    public void showToastWhenUserUnpostedATrack() {
        final EntityStateChangedEvent trackChangedEvent = EntityStateChangedEvent.fromRepost(TRACK_URN, false);

        presenter.onPlayableUpdated(trackView, trackChangedEvent);

        Assertions.assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(RuntimeEnvironment.application.getString(R.string.unposted_to_followers));
    }

    @Test
    public void toggleLikeOnTrackCallsListenerWithLikeStatus() {
        populateTrackPage();

        getHolder(trackView).likeToggle.performClick();

        verify(listener).onToggleLike(false, TRACK_URN);
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
    public void playerCloseOnPlayerCloseClick() {
        populateTrackPage();

        getHolder(trackView).close.performClick();

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
        final PropertySet track = TestPropertySets.leaveBehindForPlayer();
        populateTrackPage();

        presenter.setAdOverlay(trackView, track);

        verify(adOverlayController).initialize(track);
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
        presenter.setPlayState(trackView, TestPlayStates.error(Reason.ERROR_FAILED), true, true);

        verify(adOverlayController).clear();
    }

    @Test
    public void onPlaybackErrorShowErrorState() {
        presenter.setPlayState(trackView, TestPlayStates.error(Reason.ERROR_FAILED), true, true);

        verify(errorViewController).showError(Reason.ERROR_FAILED);
    }

    @Test
    public void onNonErrorPlaybackEventClearAnyExistingErrorState() {
        presenter.setPlayState(trackView, TestPlayStates.playing(), true, false);

        verify(errorViewController).hideError();
    }

    @Test
    public void onClickMoreButtonCallsShowOnTrackMenuController() {
        populateTrackPage();
        getHolder(trackView).more.performClick();
        verify(trackPageMenuController).show();
    }

    private TrackPageHolder getHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void populateTrackPage() {
        presenter.bindItemView(trackView, new PlayerTrackState(TestPropertySets.expectedTrackForPlayer(), true, true,
                viewVisibilityProvider));
    }
}