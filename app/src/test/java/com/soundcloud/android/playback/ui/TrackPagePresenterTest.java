package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.waveform.WaveformOperations;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
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
    private Track track;
    @Mock
    private ViewGroup container;

    private TrackPagePresenter presenter;
    private View trackView;

    @Before
    public void setUp() throws Exception {
        presenter = new TrackPagePresenter(resources, imageOperations, waveformOperations, listener);
        when(container.getContext()).thenReturn(Robolectric.application);
        trackView = presenter.createTrackPage(container, true);
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setTrackPlayState(trackView, true);

        expect(getHolder(trackView).footerPlayToggle.isChecked()).toBeTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setTrackPlayState(trackView, false);

        expect(getHolder(trackView).footerPlayToggle.isChecked()).toBeFalse();
    }

    @Test
    public void pageCreatedInFullScreenModeHasFooterControlsHidden() {
        expect(getHolder(trackView).footer.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void pageCreatedInFooterModeHasVisibleFooterControls() {
        trackView = presenter.createTrackPage(container, false);

        expect(getHolder(trackView).footer.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void settingFooterModeUpdatesFooterControlVisibility() {
        TrackPageHolder holder = getHolder(trackView);
        expect(holder.footer.getVisibility()).toEqual(View.GONE);

        presenter.setFullScreen(trackView, false);

        expect(holder.footer.getVisibility()).toEqual(View.VISIBLE);
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

        getHolder(trackView).artwork.performClick();

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

    @Test (expected=IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        presenter.onClick(mock(View.class));
    }

    private TrackPageHolder getHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void populateTrackPage() throws CreateModelException {
        presenter.populateTrackPage(trackView, TestHelper.getModelFactory().createModel(Track.class));
    }

}