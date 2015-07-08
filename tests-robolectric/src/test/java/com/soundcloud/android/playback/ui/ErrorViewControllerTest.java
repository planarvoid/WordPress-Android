package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ErrorViewControllerTest {

    @Mock private WaveformViewController waveformViewController;
    @Mock private View trackPage;
    @Mock private ViewStub errorStub;
    private View hideOnError;

    private ErrorViewController errorViewController;
    private View errorLayout;

    @Before
    public void setUp() throws Exception {
        TrackPagePresenter.TrackPageHolder holder = new TrackPagePresenter.TrackPageHolder();
        holder.waveformController = waveformViewController;
        hideOnError = new View(null);
        holder.hideOnErrorViews = Arrays.asList(hideOnError);

        errorLayout = LayoutInflater.from(Robolectric.application).inflate(R.layout.track_page_error, null);
        when(errorStub.inflate()).thenReturn(errorLayout, null);

        when(trackPage.findViewById(R.id.track_page_error_stub)).thenReturn(errorStub);
        when(trackPage.getTag()).thenReturn(holder);
        errorViewController = new ErrorViewController(trackPage);
    }

    @Test
    public void showErrorSetsTrackPageStates() {
        errorViewController.showError(Playa.Reason.ERROR_FAILED);

        verify(waveformViewController).hide();
        expect(hideOnError).toBeGone();
    }

    @Test
    public void hideErrorClearsTrackPageStates() {
        errorViewController.showError(Playa.Reason.ERROR_FAILED);

        errorViewController.hideError();

        verify(waveformViewController).show();
        expect(hideOnError).toBeVisible();
        expect(errorLayout).toBeGone();
    }

    @Test
    public void showErrorSetsMessageForConnectionError() {
        errorViewController.showError(Playa.Reason.ERROR_FAILED);

        String expected = Robolectric.application.getString(R.string.playback_error_connection);
        expect(errorLayout.findViewById(R.id.playback_error_reason)).toHaveText(expected);
    }

    @Test
    public void showErrorSetsMessageForGeneralError() {
        errorViewController.showError(Playa.Reason.ERROR_NOT_FOUND);

        String expected = Robolectric.application.getString(R.string.playback_error_unable_to_play);
        expect(errorLayout.findViewById(R.id.playback_error_reason)).toHaveText(expected);
    }

    @Test
    public void isShowingErrorIsTrueAfterShow() {
        errorViewController.showError(Playa.Reason.ERROR_FORBIDDEN);

        expect(errorViewController.isShowingError()).toBeTrue();
    }

    @Test
    public void isShowingErrorIsFalseAfterHide() {
        errorViewController.showError(Playa.Reason.ERROR_FORBIDDEN);

        errorViewController.hideError();

        expect(errorViewController.isShowingError()).toBeFalse();
    }

}