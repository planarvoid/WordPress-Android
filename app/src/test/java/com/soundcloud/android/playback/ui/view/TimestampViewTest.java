package com.soundcloud.android.playback.ui.view;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.res.Attribute;
import org.robolectric.shadows.RoboAttributeSet;

import android.widget.TextView;

import java.util.Collections;

public class TimestampViewTest extends AndroidUnitTest {

    private static final long SECONDS_5 = 5 * 1000;
    private static final long MINUTES_10 = 10 * 60 * 1000;
    private static final long MINUTES_11 = 11 * 60 * 1000;

    private TimestampView timestampView;
    private TextView progressView;
    private TextView durationView;

    @Mock private SpringSystem springSystem;
    @Mock private Spring spring;

    @Before
    public void setUp() throws Exception {
        timestampView = new TimestampView(context(), new RoboAttributeSet(Collections.<Attribute>emptyList(), shadowOf(resources()).getResourceLoader()), springSystem);
        progressView = (TextView) timestampView.findViewById(R.id.timestamp_progress);
        durationView = (TextView) timestampView.findViewById(R.id.timestamp_duration);

        when(springSystem.createSpring()).thenReturn(spring);
    }

    @Test
    public void updatesProgressWhenNotScrubbing() {
        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_10));

        assertThat(progressView).hasText("0:05");
        assertThat(durationView).hasText("10:00");
    }

    @Test
    public void doesNotUpdateProgressWhenScrubbing() {
        timestampView.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);

        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_10));

        assertThat(progressView).hasText("");
        assertThat(durationView).hasText("");
    }

    @Test
    public void setsInitialProgress() {
        timestampView.setInitialProgress(MINUTES_10);

        assertThat(progressView).hasText("0:00");
        assertThat(durationView).hasText("10:00");
    }

    @Test
    public void updatesDurationWhenProgressEventDurationIsDifferent() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_11));

        assertThat(durationView).hasText("11:00");
    }

    @Test
    public void doesNotUpdateDurationIfProgressEventDurationIsInvalid() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.setProgress(PlaybackProgress.empty());

        assertThat(durationView).hasText("10:00");
    }

    @Test
    public void updatesTimestampBasedOnScrubPosition() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.displayScrubPosition(0.5f);

        assertThat(progressView).hasText("5:00");
        assertThat(durationView).hasText("10:00");
    }

}
