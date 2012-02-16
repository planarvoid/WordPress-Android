package com.soundcloud.android.tracking;

import com.at.ATParams;
import com.at.ATTag;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Based on
 * <a href="http://blog.tomgibara.com/post/5598222730/improving-android-google-analytics-performance">
 *  Improving Google Analytics performance on Android
 * </a>, adapted for ATInternet.
 */
public class ATTracker {
    private static final String TAG = ATTracker.class.getSimpleName();

    private final ArrayList<ATParams> mQueue = new ArrayList<ATParams>();
    private boolean mQueueFlushing = false;
    private ATParams[] mEvents = null; // temporary working set, held globally to avoid pointless repeated allocations

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private final ATTag atTag;

    public ATTracker(Context context) {
        atTag = ATTag.init(context,
            context.getString(R.string.at_tracking_subdomain),
            context.getString(R.string.at_tracking_siteid),
            null
        );
        //atTag.setModePrintUrl(SoundCloudApplication.DEV_MODE);
    }

    public void track(Click click, Object... args) {
        if (click != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "track click "+click);
            enqueue(click.atParams(args));
        }
    }

    public void track(Page page, Object... args) {
        if (page != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "track page "+page);
            enqueue(page.atParams(args));
        }
    }

    // private utility methods
    private void enqueue(ATParams event) {
        synchronized (mQueue) {
            mQueue.add(event);
            if (!mQueueFlushing) {
                //noinspection unchecked
                new ATParamsTask().execute();
            }
        }
    }

    private class ATParamsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mQueue) {
                if (mQueueFlushing) return null;
                mQueueFlushing = true;
            }
            try {
                while (true) {
                    final int length;
                    synchronized (mQueue) {
                        length = mQueue.size();
                        if (length == 0) return null;

                        if (mEvents == null || mEvents.length < length) {
                            mEvents = new ATParams[mQueue.size()];
                        }
                        mQueue.toArray(mEvents);
                        mQueue.clear();
                    }
                    for (int i = 0; i < length; i++) {
                        ATParams event = mEvents[i];
                        try {
                            event.xt_sendTag();
                        } catch (Exception e) {
                            // paranoid mode
                            SoundCloudApplication.handleSilentException("error in tracking", e);
                        }
                    }
                    Arrays.fill(mEvents, null);
                }
            } finally {
                synchronized (mQueue) {
                    mQueueFlushing = false;
                }
            }
        }
    }
}