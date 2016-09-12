package com.soundcloud.android.creators.record;

import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.CustomFontTextView;

import android.content.Context;
import android.util.AttributeSet;

import java.util.concurrent.TimeUnit;

/**
 * Simple chronometer that handles its own formatting and tries to minimize duplicate results
 */

public class ChronometerView extends CustomFontTextView {
    private long durationSec = -1L, progressSec = -1L;
    private String durationString;

    private int mode;
    private static final int MODE_EMPTY = 0;
    private static final int MODE_DURATION_ONLY = 1;
    private static final int MODE_PLAYBACK = 2;

    @SuppressWarnings("UnusedDeclaration")
    public ChronometerView(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ChronometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ChronometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clear() {
        setText("");
        mode = MODE_EMPTY;
    }

    public void setDurationOnly(long ms) {
        if (setDuration(ms) || mode != MODE_DURATION_ONLY) {
            mode = MODE_DURATION_ONLY;
            setText(durationString);
        }
    }

    public void setPlaybackProgress(long ms, long durationMs) {
        setDuration(durationMs);
        final long newProgressSec = ms / 1000;
        if (newProgressSec != progressSec || mode != MODE_PLAYBACK) {
            progressSec = newProgressSec;
            mode = MODE_PLAYBACK;
            setText(new StringBuilder()
                            .append(ScTextUtils.formatTimestamp(ms, TimeUnit.MILLISECONDS))
                            .append(" / ")
                            .append(durationString));

        }
    }

    private boolean setDuration(long ms) {
        final long newDurationSec = ms / 1000;
        if (newDurationSec != durationSec) {
            durationSec = newDurationSec;
            durationString = ScTextUtils.formatTimestamp(newDurationSec, TimeUnit.SECONDS);
            return true;
        } else {
            return false;
        }
    }
}
