package com.soundcloud.android.view.create;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import com.soundcloud.android.activity.ScCreate;
import com.soundcloud.android.utils.CloudUtils;

/**
 * Simple chronometer that handles its own formatting and tries to minimize duplicate results
 */

public class Chronometer extends TextView {

    private long mDurationSec, mProgressSec;
    private String mDurationString;
    private ScCreate mCreateRef;

    private int mode;
    private static final int MODE_EMPTY = 0;
    private static final int MODE_DURATION_ONLY = 1;
    private static final int MODE_PLAYBACK = 2;

    public Chronometer(Context context) {
        super(context);
    }

    public Chronometer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Chronometer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clear(){
        setText("");
        mode = MODE_EMPTY;
    }

    public void setRecordProgress(long ms){
        if (mode != MODE_DURATION_ONLY || setDuration(ms)){
            mode = MODE_DURATION_ONLY;
            setText(mDurationString);
        }

    }

    public void setPlaybackProgress(long ms, long durationMs){
        setDuration(durationMs);
        final long newProgressSec = ms / 1000;
        if (newProgressSec != mProgressSec || mode != MODE_PLAYBACK) {
            mProgressSec = newProgressSec;
            mode = MODE_PLAYBACK;
            setText(new StringBuilder()
                .append(CloudUtils.formatTimestamp(ms))
                .append(" / ")
                .append(mDurationString));

        }
    }

    private boolean setDuration(long ms){
        final long newDurationSec = ms / 1000;
        if (newDurationSec != mDurationSec) {
            mDurationSec = newDurationSec;
            mDurationString = CloudUtils.formatTimestamp(ms);
            return true;
        }
        return false;
    }
}
