package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.ads.AdOverlayController.Factory;
import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.ads.AdOverlayController.AdOverlayListener;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagePresenterTest {

    private static final int DURATION = 20000;
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private Resources resources;
    @Mock private WaveformOperations waveformOperations;
    @Mock private TrackPageListener listener;
    @Mock private ViewGroup container;
    @Mock private WaveformViewController.Factory waveformFactory;
    @Mock private WaveformViewController waveformViewController;
    @Mock private PlayerArtworkController.Factory artworkFactory;
    @Mock private PlayerArtworkController artworkController;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private Factory leaveBehindControllerFactory;
    @Mock private AdOverlayController adOverlayController;
    @Mock private SkipListener skipListener;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;
    @Mock private FeatureFlags featureFlags;

    @Mock private TrackPageMenuController.Factory trackMenuControllerFactory;
    @Mock private TrackPageMenuController trackPageMenuController;
    @Mock private PlaybackProgress playbackProgress;

    private TrackPagePresenter presenter;
    private View trackView;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB); // Required by nineoldandroids
        presenter = new TrackPagePresenter(waveformOperations, listener, waveformFactory,
                artworkFactory, playerOverlayControllerFactory, trackMenuControllerFactory, leaveBehindControllerFactory, featureFlags);
        when(container.getContext()).thenReturn(Robolectric.application);
        when(waveformFactory.create(any(WaveformView.class), any(AdOverlayController.class))).thenReturn(waveformViewController);
        when(artworkFactory.create(any(PlayerTrackArtworkView.class))).thenReturn(artworkController);
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(playerOverlayController);
        when(trackMenuControllerFactory.create(any(View.class))).thenReturn(trackPageMenuController);
        when(leaveBehindControllerFactory.create(any(View.class), any(AdOverlayListener.class))).thenReturn(adOverlayController);
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
        expect(getHolder(trackView).likeToggle).toBeChecked();
        expect(getHolder(trackView).likeToggle).toHaveText("1");
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).footerPlayToggle).toBeChecked();
    }

    @Test
    public void playingStateShowsBackgroundOnTitle() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).title.isShowingBackground()).toBeTrue();
    }

    @Test
    public void playingStateShowsBackgroundOnUser() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).user.isShowingBackground()).toBeTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).footerPlayToggle).not.toBeChecked();
    }

    @Test
    public void pausedStateHidesBackgroundOnTitle() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).title.isShowingBackground()).toBeFalse();
    }

    @Test
    public void playingStateHidesBackgroundOnUser() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).user.isShowingBackground()).toBeFalse();
    }

    @Test
    public void playingStateShowsBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).timestamp.isShowingBackground()).toBeTrue();
    }

    @Test
    public void pauseStateHidesBackgroundOnTimestamp() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        expect(getHolder(trackView).timestamp.isShowingBackground()).toBeFalse();
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET, 10, 20), true, true);
        verify(waveformViewController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithCurrentTrackDoesNotResetProgress() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET,  10, 20), true, true);

        verify(waveformViewController, never()).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateWithProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET,  10, 20), true, true);
        verify(artworkController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET,  10, 20), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET,  10, 20), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackResetProgress() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET,  10, 20), false, true);

        verify(waveformViewController).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsBufferingStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), true, true);
        verify(waveformViewController).showBufferingState();
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsIdleStateWithProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, Urn.forTrack(123L), 10, 20), true, true);
        verify(artworkController).showIdleState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void bufferingStateWithOtherTrackResetProgress() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), false, true);

        verify(waveformViewController).setProgress(PlaybackProgress.empty());
    }

    @Test
    public void bufferingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void bufferingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), false, true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), false, true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void setExpandedShouldHideFooterControl() {
        presenter.setExpanded(trackView);
        expect(getHolder(trackView).footer).toBeGone();
    }

    @Test
    public void setCollapsedShouldShowFooterControl() {
        presenter.setCollapsed(trackView);
        expect(getHolder(trackView).footer).toBeVisible();
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
    public void updateAssociationsWithLikedPropertyUpdatesLikeToggle() {
        getHolder(trackView).likeToggle.setEnabled(false); // Toggle disable whilst updating
        final PlayableUpdatedEvent playableUpdatedEvent = PlayableUpdatedEvent.forLike(TRACK_URN, true, 1);

        presenter.onPlayableUpdated(trackView, playableUpdatedEvent);

        expect(getHolder(trackView).likeToggle).toBeChecked();
    }

    @Test
    public void updateAssociationsWithLikedCountPropertyUpdatesLikeCountBelow10k() {
        final PlayableUpdatedEvent playableUpdatedEvent = PlayableUpdatedEvent.forLike(TRACK_URN, true, 9999);

        presenter.onPlayableUpdated(trackView, playableUpdatedEvent);

        expect(getHolder(trackView).likeToggle).toHaveText("9,999");
    }

    @Test
    public void updateAssociationsWithRepostedPropertyUpdatesRepostStatusOnMenuController() throws Exception {
        final PlayableUpdatedEvent playableUpdatedEvent = PlayableUpdatedEvent.forRepost(TRACK_URN, true, 1);

        presenter.onPlayableUpdated(trackView, playableUpdatedEvent);

        verify(trackPageMenuController).setIsUserRepost(true);
    }

    @Test
    public void showToastWhenUserRepostedATrack() {
        final PlayableUpdatedEvent playableUpdatedEvent = PlayableUpdatedEvent.forRepost(TRACK_URN, true, 1);

        presenter.onPlayableUpdated(trackView, playableUpdatedEvent);

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.reposted_to_followers);
    }

    @Test
    public void showToastWhenUserUnpostedATrack() {
        final PlayableUpdatedEvent playableUpdatedEvent = PlayableUpdatedEvent.forRepost(TRACK_URN, false, 1);

        presenter.onPlayableUpdated(trackView, playableUpdatedEvent);

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.unposted_to_followers);
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

        expect(getHolder(trackView).nextButton).toBeVisible();
        expect(getHolder(trackView).previousButton).toBeInvisible();
    }

    @Test
    public void onPositionSetHidesNextButtonForLastTrack() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 4, 5);

        expect(getHolder(trackView).nextButton).toBeInvisible();
        expect(getHolder(trackView).previousButton).toBeVisible();
    }

    @Test
    public void onPositionSetShowsBothNavigationButtonsForTrackInMiddleOfQueue() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 2, 5);

        expect(getHolder(trackView).nextButton).toBeVisible();
        expect(getHolder(trackView).previousButton).toBeVisible();
    }

    @Test
    public void onPositionSetHidesBothNavigationButtonsForSingleTrack() {
        populateTrackPage();

        presenter.onPositionSet(trackView, 0, 1);

        expect(getHolder(trackView).nextButton).toBeInvisible();
        expect(getHolder(trackView).previousButton).toBeInvisible();
    }

    @Test
    public void setLeaveBehindInitializesLeaveBehindController() throws Exception {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        final PropertySet track = TestPropertySets.leaveBehindForPlayer();
        populateTrackPage();

        presenter.setAdOverlay(trackView, track);

        verify(adOverlayController).initialize(track);
    }

    @Test
    public void clearLeaveBehindClearsLeaveBehindUsingController() throws Exception {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        populateTrackPage();

        presenter.clearAdOverlay(trackView);

        verify(adOverlayController).clear();

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        presenter.onClick(new View(Robolectric.application));
    }

    @Test
    public void onClickMoreButtonCallsDismissOnLeaveBehindController() {
        populateTrackPage();
        getHolder(trackView).more.performClick();
        verify(adOverlayController).clear();
    }

    @Test
    public void onPauseDismissOnLeaveBehindController() {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true, true);

        verify(adOverlayController).clear();
    }

    @Test
    public void onPlayShowsInBackgroundDoesNotShowLeaveBehind() {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, false);

        verify(adOverlayController, never()).show();
    }

    @Test
    public void onPlayShowsOnLeaveBehindController() {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true, true);

        verify(adOverlayController).show(true);
    }

    @Test
    public void onPlaybackErrorDismissOnLeaveBehindController() {
        when(featureFlags.isEnabled(Feature.LEAVE_BEHIND)).thenReturn(true);
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED), true, true);

        verify(adOverlayController).clear();
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
        presenter.bindItemView(trackView, TestPropertySets.expectedTrackForPlayer(), true, true, viewVisibilityProvider);
    }
}