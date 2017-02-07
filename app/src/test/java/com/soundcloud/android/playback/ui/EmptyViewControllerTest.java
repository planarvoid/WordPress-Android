package com.soundcloud.android.playback.ui;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.RoboLayoutInflater;

import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import java.util.Collections;

public class EmptyViewControllerTest extends AndroidUnitTest {

    @Mock private WaveformViewController waveformViewController;
    @Mock private TextView footerUser;
    @Mock private TextView footerTitle;
    @Mock private View trackPage;
    @Mock private ViewStub emptyStub;

    private View hideOnEmpty;
    private EmptyViewController emptyViewController;
    private View emptyLayout;

    @Before
    public void setUp() throws Exception {
        TrackPagePresenter.TrackPageHolder holder = new TrackPagePresenter.TrackPageHolder();
        holder.footerUser = footerUser;
        holder.footerTitle = footerTitle;
        holder.waveformController = waveformViewController;
        hideOnEmpty = new View(context());
        holder.hideOnEmptyViews = Collections.singletonList(hideOnEmpty);

        emptyLayout = RoboLayoutInflater.from(activity()).inflate(R.layout.track_page_empty, null);
        when(emptyStub.inflate()).thenReturn(emptyLayout);

        when(trackPage.findViewById(R.id.track_page_empty_stub)).thenReturn(emptyStub);
        when(trackPage.getTag()).thenReturn(holder);
        emptyViewController = new EmptyViewController(trackPage);
    }

    @Test
    public void showSetsTrackPageStates() {
        emptyViewController.show();

        verify(waveformViewController).hide();
        assertThat(hideOnEmpty).isGone();
    }

    @Test
    public void hideErrorClearsTrackPageStates() {
        emptyViewController.show();
        emptyViewController.hide();

        verify(waveformViewController).show();
        assertThat(hideOnEmpty).isVisible();
        assertThat(emptyLayout).isGone();
    }
}
