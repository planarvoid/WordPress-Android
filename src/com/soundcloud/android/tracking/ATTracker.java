package com.soundcloud.android.tracking;

import com.at.ATParams;
import com.at.ATTag;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;

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

    // identified visitors
    private static final String USER_ID = "an";
    private static final String PLAN    = "ac";

    // custom variables
    private static final String CUSTOM_SOUNDS      = "1";
    private static final String CUSTOM_LIKES       = "2";
    private static final String CUSTOM_FOLLOWINGS  = "3";
    private static final String CUSTOM_FOLLOWERS   = "4";
    private static final String CUSTOM_CREATED_AT  = "5";
    private static final String CUSTOM_FB_SIGNUP   = "6";

    private final ArrayList<ATParams> mQueue = new ArrayList<ATParams>();
    private boolean mQueueFlushing = false;
    private ATParams[] mEvents = null; // temporary working set, held globally to avoid pointless repeated allocations

    final private SoundCloudApplication app;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private final ATTag atTag;

    public ATTracker(SoundCloudApplication context) {
        atTag = ATTag.init(context,
            context.getString(R.string.at_tracking_subdomain),
            context.getString(R.string.at_tracking_siteid),
            null
        );

        this.app = context;
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



    private void enqueue(final ATParams atParams) {
        final ATParams event = addUserExtras(atParams);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "tracking "+event);
        synchronized (mQueue) {
            mQueue.add(event);
            if (!mQueueFlushing) {
                //noinspection unchecked
                new ATParamsTask().execute();
            }
        }
    }

    private ATParams addUserExtras(ATParams event) {
        final User user = app.getLoggedInUser();
        if (user.id > 0) {
            // identified visitor
            event.put(USER_ID, String.valueOf(user.id));

            // custom vars
            setCustom(event, CUSTOM_SOUNDS, user.track_count);
            setCustom(event, CUSTOM_FOLLOWERS, user.followers_count);
            setCustom(event, CUSTOM_FOLLOWINGS, user.followings_count);

            if (user.via != null && user.via.isFacebook()) {
                event.setCustomCritera(CUSTOM_FB_SIGNUP, "1");
            }
        }
        // TODO remaining variables - needs db migration (add plan + favorite_count)
        return event;
    }


    private static void setCustom(ATParams event, String name, int value) {
        if (value >= 0) {
            event.setCustomCritera(name, String.valueOf(value));
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