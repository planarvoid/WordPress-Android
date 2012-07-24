package com.soundcloud.android.view.create;

import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Simple chronometer that handles its own formatting and tries to minimize duplicate results
 */

public class Chronometer extends TextView {
    private long mDurationSec = -1l, mProgressSec = -1l;
    private String mDurationString;

    private int mode;
    private static final int MODE_EMPTY = 0;
    private static final int MODE_DURATION_ONLY = 1;
    private static final int MODE_PLAYBACK = 2;

    @SuppressWarnings("UnusedDeclaration")
    public Chronometer(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public Chronometer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public Chronometer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clear() {
        setText("");
        mode = MODE_EMPTY;
    }

    public void setDurationOnly(long ms) {
        if (mode != MODE_DURATION_ONLY || setDuration(ms)) {
            mode = MODE_DURATION_ONLY;
            setText(mDurationString);
        }
    }

    public void setPlaybackProgress(long ms, long durationMs) {
        setDuration(durationMs);
        final long newProgressSec = ms / 1000;
        if (newProgressSec != mProgressSec || mode != MODE_PLAYBACK) {
            mProgressSec = newProgressSec;
            mode = MODE_PLAYBACK;
            setText(new StringBuilder()
                    .append(ScTextUtils.formatTimestamp(ms))
                    .append(" / ")
                    .append(mDurationString));

        }
    }

    private boolean setDuration(long ms) {
        final long newDurationSec = ms / 1000;
        if (newDurationSec != mDurationSec) {
            mDurationSec = newDurationSec;
            mDurationString = ScTextUtils.formatTimestamp(ms);
            return true;
        } else {
            return false;
        }
    }
}
