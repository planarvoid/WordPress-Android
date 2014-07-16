package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class TimestampView extends RelativeLayout implements ProgressAware, OnScrubListener {

    private boolean suppressProgress;
    private long duration;

    private final TextView progressText;
    private final TextView durationText;
    private final View background;

    public TimestampView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.timestamp_layout, this, true);
        progressText = (TextView) findViewById(R.id.timestamp_progress);
        durationText = (TextView) findViewById(R.id.timestamp_duration);
        background = findViewById(R.id.timestamp_background);
    }

    public void setInitialProgress(long duration) {
        this.duration = duration;
        progressText.setText(format(0));
        durationText.setText(format(duration));
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        if (progress.getDuration() != duration) {
            duration = progress.getDuration();
        }
        if (!suppressProgress) {
            progressText.setText(format(progress.getPosition()));
            durationText.setText(format(duration));
        }
    }

    public void showBackground(boolean visible) {
        background.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @VisibleForTesting
    public boolean isShowingBackground() {
        return background.getVisibility() == View.VISIBLE;
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        long scrubTime = (long) (scrubPosition * duration);
        progressText.setText(format(scrubTime));
    }

    private String format(long millis) {
        return ScTextUtils.formatTimestamp(millis, TimeUnit.MILLISECONDS);
    }

}
