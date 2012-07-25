package com.soundcloud.android.tracking;

import static com.soundcloud.android.utils.NetworkConnectivityListener.State;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.at.ATParams;
import com.at.ATTag;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.model.Plan;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.NetworkConnectivityListener;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Based on
 * <a href="http://blog.tomgibara.com/post/5598222730/improving-android-google-analytics-performance">
 *  Improving Google Analytics performance on Android
 * </a>, adapted for ATInternet.
 */
public class ATTracker implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = ATTracker.class.getSimpleName();

    // identified visitors
    private static final String USER_ID = "an";
    private static final String PLAN    = "ac";

    // custom variables
    private static final String CUSTOM_SOUNDS      = "1";
    private static final String CUSTOM_LIKES       = "2";
    private static final String CUSTOM_FOLLOWINGS  = "3";
    private static final String CUSTOM_FOLLOWERS   = "4";
    private static final String CUSTOM_CREATED_AT  = "5";  // TODO created_at: needs API changes
    private static final String CUSTOM_FB_SIGNUP   = "6";

    private final ArrayList<ATParams> mQueue = new ArrayList<ATParams>();
    private boolean mIsEnabled;
    private boolean mQueueFlushing;
    private ATParams[] mEvents; // temporary working set, held globally to avoid pointless repeated allocations

    final private SoundCloudApplication app;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private final ATTag atTag;

    public ATTracker(SoundCloudApplication context) {
        atTag = ATTag.init(context,
            context.getString(R.string.at_tracking_subdomain),
            context.getString(R.string.at_tracking_siteid),
            null
        );
        atTag.setOfflineMode(ATTag.OfflineMode.OfflineModeRequired);
        this.app = context;

        final Handler handler = new Handler() {
            @Override public void handleMessage(Message msg) {
                State old = State.values()[msg.arg1];
                State current = State.values()[msg.arg2];
                if (old != State.CONNECTED && current == State.CONNECTED) {
                    try {
                        ATTag.sendNow();
                    } catch (Exception e) {
                        SoundCloudApplication.handleSilentException("error in ATTag#sendNow()", e);
                    }
                }
            }
        };

        NetworkConnectivityListener connectivity = new NetworkConnectivityListener();
        connectivity.registerHandler(handler, 0);
        connectivity.startListening(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        mIsEnabled = preferences.getBoolean(Settings.ANALYTICS, true);
    }

    public void track(Event event, Object... args) {
        if (!mIsEnabled) { return; }

        if (event != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "track event "+event);
            enqueue(event.atParams(args));
        }
    }

    private void enqueue(final ATParams atParams) {
        if (atParams == null) return;

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
            Plan plan = user.getPlan();
            if (plan != Plan.UNKNOWN) {
                event.put(PLAN, String.valueOf(plan.id));
            }
            // custom vars
            setCustom(event, CUSTOM_SOUNDS, user.track_count);
            setCustom(event, CUSTOM_FOLLOWERS, user.followers_count);
            setCustom(event, CUSTOM_FOLLOWINGS, user.followings_count);
            setCustom(event, CUSTOM_LIKES, user.public_favorites_count);
            event.setCustomCritera(CUSTOM_FB_SIGNUP, user.via != null && user.via.isFacebook() ? "1" : "0");
        }

        return event;
    }

    private static void setCustom(ATParams event, String name, int value) {
        if (value >= 0) {
            event.setCustomCritera(name, String.valueOf(value));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(Settings.ANALYTICS)) {
            mIsEnabled = sharedPreferences.getBoolean(Settings.ANALYTICS, true);
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