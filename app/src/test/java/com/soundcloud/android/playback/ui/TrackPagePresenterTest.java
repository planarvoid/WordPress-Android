package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.ui.PlayerArtworkController.PlayerArtworkControllerFactory;
import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.playback.ui.view.WaveformViewControllerFactory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
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

    @Mock
    private Resources resources;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private WaveformOperations waveformOperations;
    @Mock
    private TrackPageListener listener;
    @Mock
    private ViewGroup container;
    @Mock
    private WaveformViewControllerFactory waveformFactory;
    @Mock
    private WaveformViewController waveformViewController;
    @Mock
    private PlayerArtworkControllerFactory artworkFactory;
    @Mock
    private PlayerArtworkController artworkController;
    @Mock
    private PlaybackProgress playbackProgress;

    private TrackPagePresenter presenter;
    private View trackView;

    @Before
    public void setUp() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK", String.valueOf(Build.VERSION_CODES.HONEYCOMB)); // Required by nineoldandroids
        presenter = new TrackPagePresenter(resources, imageOperations, waveformOperations, listener, waveformFactory, artworkFactory);
        when(container.getContext()).thenReturn(Robolectric.application);
        when(waveformFactory.create(any(WaveformView.class))).thenReturn(waveformViewController);
        when(artworkFactory.create(any(PlayerArtworkView.class))).thenReturn(artworkController);
        trackView = presenter.createTrackPage(container);
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true);
        expect(getHolder(trackView).footerPlayToggle.isChecked()).toBeTrue();
    }

    @Test
    public void playingStateShowsBackgroundOnTitle() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true);
        expect(getHolder(trackView).title.isShowingBackground()).toBeTrue();
    }

    @Test
    public void playingStateShowsBackgroundOnUser() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true);
        expect(getHolder(trackView).user.isShowingBackground()).toBeTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true);
        expect(getHolder(trackView).footerPlayToggle.isChecked()).toBeFalse();
    }

    @Test
    public void pausedStateSetsHidesBackgroundOnTitle() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true);
        expect(getHolder(trackView).title.isShowingBackground()).toBeFalse();
    }

    @Test
    public void playingStateHidesBackgroundOnUser() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true);
        expect(getHolder(trackView).user.isShowingBackground()).toBeFalse();
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, 10, 20), true);
        verify(waveformViewController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithCurrentTrackShowsPlayingStateWithProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, 10, 20), true);
        verify(artworkController).showPlayingState(eq(new PlaybackProgress(10, 20)));
    }

    @Test
    public void playingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, 10, 20), false);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void playingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, 10, 20), false);
        verify(artworkController).showSessionActiveState();
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsBufferingStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), true);
        verify(waveformViewController).showBufferingState();
    }

    @Test
    public void bufferingStateWithCurrentTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), true);
        verify(artworkController).showSessionActiveState();
    }

    @Test
    public void bufferingStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), false);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void bufferingStateWithOtherTrackShowsPlayingStateWithoutProgressOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE), false);
        verify(artworkController).showSessionActiveState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithCurrentTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), true);
        verify(artworkController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnWaveform() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), false);
        verify(waveformViewController).showIdleState();
    }

    @Test
    public void idleStateWithOtherTrackShowsIdleStateOnArtwork() {
        presenter.setPlayState(trackView, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE), false);
        verify(artworkController).showIdleState();
    }

    @Test
    public void pageCreatedInFullScreenModeHasFooterControlsHidden() {
        presenter.setExpandedMode(true);
        trackView = presenter.createTrackPage(container);
        expect(getHolder(trackView).footer.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void pageCreatedInFooterModeHasVisibleFooterControls() {
        presenter.setExpandedMode(false);
        trackView = presenter.createTrackPage(container);
        expect(getHolder(trackView).footer.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void settingFooterModeUpdatesFooterControlVisibility() {
        presenter.setExpandedMode(true);
        trackView = presenter.createTrackPage(container);
        TrackPageHolder holder = getHolder(trackView);
        expect(holder.footer.getVisibility()).toEqual(View.GONE);

        presenter.setCollapsed(trackView);

        expect(holder.footer.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void createTrackPageSetsArtworkAsScrubListenerOnWaveformController() {
        verify(waveformViewController).addScrubListener(artworkController);
    }

    @Test
    public void populateTrackViewSetsProgressOnWaveformController() throws CreateModelException {
        populateTrackPage();
        verify(waveformViewController).setProgress(playbackProgress);
    }

    @Test
    public void setProgressSetsProgressOnWaveformController() throws CreateModelException {
        presenter.setProgress(trackView, playbackProgress);
        verify(waveformViewController).setProgress(playbackProgress);
    }

    @Test
    public void resetProgressResetsProgressOnWaveformController() throws CreateModelException {
        presenter.resetProgress(trackView);
        verify(waveformViewController).setProgress(eq(PlaybackProgress.empty()));
    }

    @Test
    public void populateTrackViewSetsProgressOnArtwork() throws CreateModelException {
        populateTrackPage();
        verify(artworkController).setProgress(playbackProgress);
    }

    @Test
    public void setProgressSetsProgressOnArtworkController() throws CreateModelException {
        presenter.setProgress(trackView, playbackProgress);
        verify(artworkController).setProgress(playbackProgress);
    }

    @Test
    public void resetProgressResetsProgressOnArtworkController() throws CreateModelException {
        presenter.resetProgress(trackView);
        verify(artworkController).setProgress(eq(PlaybackProgress.empty()));
    }

    @Test
    public void togglePlayOnFooterToggleClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).footerPlayToggle.performClick();

        verify(listener).onTogglePlay();
    }

    @Test
    public void togglePlayOnTrackPageArtworkClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).artworkView.performClick();

        verify(listener).onTogglePlay();
    }

    @Test
    public void nextOnTrackPagerNextClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).nextTouch.performClick();

        verify(listener).onNext();
    }

    @Test
    public void nextOnTrackPagerPreviousClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).previousTouch.performClick();

        verify(listener).onPrevious();
    }

    @Test
    public void footerTapOnFooterControlsClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).footer.performClick();

        verify(listener).onFooterTap();
    }

    @Test
    public void playerCloseOnPlayerCloseClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).close.performClick();

        verify(listener).onPlayerClose();
    }

    @Test
    public void playerCloseOnPlayerBottomCloseClick() throws CreateModelException {
        populateTrackPage();

        getHolder(trackView).bottomClose.performClick();

        verify(listener).onPlayerClose();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        presenter.onClick(mock(View.class));
    }

    private TrackPageHolder getHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void populateTrackPage() throws CreateModelException {
        presenter.populateTrackPage(trackView, buildPlayerTrack(), playbackProgress);
    }

    private PlayerTrack buildPlayerTrack() {
        return new PlayerTrack(PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.WAVEFORM_URL.bind("http://waveform.url"),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.DURATION.bind(123456)
        ));
    }
}