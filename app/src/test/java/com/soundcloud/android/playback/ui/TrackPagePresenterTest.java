package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagePresenterTest {

    @Mock
    private ImageOperations imageOperations;
    @Mock
    private Resources resources;
    @Mock
    private TrackPagePresenter.Listener listener;
    @Mock
    private Track track;
    @Mock
    private ViewGroup container;

    private TrackPagePresenter presenter;
    private View trackView;

    @Before
    public void setUp() throws Exception {
        presenter = new TrackPagePresenter(imageOperations, resources);
        presenter.setListener(listener);
        when(container.getContext()).thenReturn(Robolectric.application);
        trackView = presenter.createTrackPage(container);
    }

    @Test
    public void playingStateSetsToggleChecked() {
        presenter.setPlayState(trackView, true);

        ToggleButton toggle = ((TrackPageHolder) trackView.getTag()).footerPlayToggle;
        expect(toggle.isChecked()).toBeTrue();
    }

    @Test
    public void pausedStateSetsToggleUnchecked() {
        presenter.setPlayState(trackView, false);

        ToggleButton toggle = ((TrackPageHolder) trackView.getTag()).footerPlayToggle;
        expect(toggle.isChecked()).toBeFalse();
    }

}