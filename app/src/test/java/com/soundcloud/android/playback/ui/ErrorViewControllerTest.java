package com.soundcloud.android.playback.ui;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.stations.StartStationPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import java.util.Arrays;

public class ErrorViewControllerTest extends AndroidUnitTest {

    @Mock private WaveformViewController waveformViewController;
    @Mock private View trackPage;
    @Mock private ViewStub errorStub;
    private View hideOnError;

    private ErrorViewController errorViewController;
    private View errorLayout;
    private StartStationPresenter startStationPresenter;

    @Before
    public void setUp() throws Exception {
        TrackPagePresenter.TrackPageHolder holder = new TrackPagePresenter.TrackPageHolder();
        holder.waveformController = waveformViewController;
        hideOnError = new View(context());
        holder.hideOnErrorViews = Arrays.asList(hideOnError);

        errorLayout = LayoutInflater.from(context()).inflate(R.layout.track_page_error, null);
        when(errorStub.inflate()).thenReturn(errorLayout);

        when(trackPage.findViewById(R.id.track_page_error_stub)).thenReturn(errorStub);
        when(trackPage.getTag()).thenReturn(holder);
        errorViewController = new ErrorViewController(startStationPresenter, trackPage);
    }

    @Test
    public void showErrorSetsTrackPageStates() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        verify(waveformViewController).hide();
        assertThat(hideOnError).isGone();
    }

    @Test
    public void hideErrorClearsTrackPageStates() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        errorViewController.hideError();

        verify(waveformViewController).show();
        assertThat(hideOnError).isVisible();
        assertThat(errorLayout).isGone();
    }

    @Test
    public void showErrorSetsMessageForConnectionError() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        String expected = resources().getString(R.string.playback_error_connection);
        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_reason)).hasText(expected);
    }

    @Test
    public void showErrorSetsMessageForUnplayableError() {
        errorViewController.showError(ErrorViewController.ErrorState.UNPLAYABLE);

        String expected = resources().getString(R.string.playback_error_unable_to_play);
        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_reason)).hasText(expected);
    }

    @Test
    public void showErrorSetsMessageForBlockedError() {
        errorViewController.showError(ErrorViewController.ErrorState.BLOCKED);

        String expected = resources().getString(R.string.playback_error_blocked);
        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_reason)).hasText(expected);
    }

    @Test
    public void showErrorShowsStationsButtonForBlockedError() {
        errorViewController.showError(ErrorViewController.ErrorState.BLOCKED);

        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_station_button)).isVisible();
    }

    @Test
    public void showErrorHidesStationsButtonForUnplayableError() {
        errorViewController.showError(ErrorViewController.ErrorState.UNPLAYABLE);

        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_station_button)).isNotVisible();
    }

    @Test
    public void showErrorHidesStationsButtonForConnectionError() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        assertThat((TextView) errorLayout.findViewById(R.id.playback_error_station_button)).isNotVisible();
    }

    @Test
    public void isShowingErrorIsTrueAfterShow() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        assertThat(errorViewController.isShowingError()).isTrue();
    }

    @Test
    public void isShowingErrorIsFalseAfterHide() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        errorViewController.hideError();

        assertThat(errorViewController.isShowingError()).isFalse();
    }

    @Test
    public void hideNonBlockedErrorsHidesFailedError() {
        errorViewController.showError(ErrorViewController.ErrorState.FAILED);

        errorViewController.hideNonBlockedErrors();

        assertThat(errorViewController.isShowingError()).isFalse();
    }

    @Test
    public void hideNonBlockedErrorsDoesNotHideBlockedError() {
        errorViewController.showError(ErrorViewController.ErrorState.BLOCKED);

        errorViewController.hideNonBlockedErrors();

        assertThat(errorViewController.isShowingError()).isTrue();
    }

}
