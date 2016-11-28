package com.soundcloud.android.playback.ui.view;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.widget.TextView;

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
        timestampView = new TimestampView(context(), attributeSet(), springSystem);
        progressView = (TextView) timestampView.findViewById(R.id.timestamp_progress);
        durationView = (TextView) timestampView.findViewById(R.id.timestamp_duration);

        when(springSystem.createSpring()).thenReturn(spring);
    }

    @Test
    public void updatesProgressWhenNotScrubbing() {
        timestampView.setInitialProgress(MINUTES_11, MINUTES_10);
        timestampView.setProgress(TestPlaybackProgress.getPlaybackProgress(SECONDS_5, MINUTES_10));

        assertThat(progressView).hasText("0:05");
        assertThat(durationView).hasText("11:00");
    }

    @Test
    public void doesNotUpdateProgressWhenScrubbing() {
        timestampView.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);

        timestampView.setProgress(TestPlaybackProgress.getPlaybackProgress(SECONDS_5, MINUTES_10));

        assertThat(progressView).hasText("");
        assertThat(durationView).hasText("");
    }

    @Test
    public void setsInitialProgress() {
        timestampView.setInitialProgress(MINUTES_11, MINUTES_10);

        assertThat(progressView).hasText("0:00");
        assertThat(durationView).hasText("11:00");
    }

    @Test
    public void updatesTimestampBasedOnBoundedScrubPositionRoundedUpToNearestSecond() {
        timestampView.setInitialProgress(MINUTES_11, 112832);

        timestampView.displayScrubPosition(0.2f, .26296297f);

        assertThat(progressView).hasText("0:30");
        assertThat(durationView).hasText("11:00");
    }

    @Test
    public void clearProgressSetsTimeToZero() {
        timestampView.setInitialProgress(MINUTES_11, MINUTES_10);
        timestampView.setProgress(TestPlaybackProgress.getPlaybackProgress(SECONDS_5, MINUTES_10));
        timestampView.clearProgress();

        assertThat(progressView).hasText("0:00");
        assertThat(durationView).hasText("11:00");

    }
}
