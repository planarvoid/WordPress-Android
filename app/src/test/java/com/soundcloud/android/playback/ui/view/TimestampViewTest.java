package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.android.util.TestAttributeSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class TimestampViewTest {

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
        timestampView = new TimestampView(Robolectric.application, new TestAttributeSet(), springSystem);
        progressView = (TextView) timestampView.findViewById(R.id.timestamp_progress);
        durationView = (TextView) timestampView.findViewById(R.id.timestamp_duration);

        when(springSystem.createSpring()).thenReturn(spring);
    }

    @Test
    public void updatesProgressWhenNotScrubbing() {
        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_10));

        expect(progressView).toHaveText("0:05");
        expect(durationView).toHaveText("10:00");
    }

    @Test
    public void doesNotUpdateProgressWhenScrubbing() {
        timestampView.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);

        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_10));

        expect(progressView).toHaveText("");
        expect(durationView).toHaveText("");
    }

    @Test
    public void setsInitialProgress() {
        timestampView.setInitialProgress(MINUTES_10);

        expect(progressView).toHaveText("0:00");
        expect(durationView).toHaveText("10:00");
    }

    @Test
    public void updatesDurationWhenProgressEventDurationIsDifferent() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.setProgress(new PlaybackProgress(SECONDS_5, MINUTES_11));

        expect(durationView).toHaveText("11:00");
    }

    @Test
    public void doesNotUpdateDurationIfProgressEventDurationIsInvalid() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.setProgress(PlaybackProgress.empty());

        expect(durationView).toHaveText("10:00");
    }

    @Test
    public void updatesTimestampBasedOnScrubPosition() {
        timestampView.setInitialProgress(MINUTES_10);

        timestampView.displayScrubPosition(0.5f);

        expect(progressView).toHaveText("5:00");
        expect(durationView).toHaveText("10:00");
    }

}