package com.soundcloud.android.analytics;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.State;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The engine which drives sending analytics. Important that all analytics providers to this engine
 * do not rely on singletons to do their work. It should be possible to create multiple providers and open sessions,
 * close sessions and handle the events being sent in a multi-threaded environment.
 * <p/>
 * The analytics engine should be used in aspects located in the aspect folder under src/main/java
 */
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = AnalyticsEngine.class.getSimpleName();

    @VisibleForTesting
    protected static AtomicBoolean sActivitySessionOpen = new AtomicBoolean();


    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;
    private boolean mAnalyticsPreferenceEnabled;
    private CloudPlayerStateWrapper mCloudPlaybackStateWrapper;

    public AnalyticsEngine(Context context) {
        this(new AnalyticsProperties(context.getResources()), new CloudPlayerStateWrapper(),
                PreferenceManager.getDefaultSharedPreferences(context),
                new LocalyticsAnalyticsProvider(context.getApplicationContext()));
    }

    @VisibleForTesting
    protected AnalyticsEngine(AnalyticsProperties analyticsProperties, CloudPlayerStateWrapper cloudPlaybackStateWrapper,
                              SharedPreferences sharedPreferences, AnalyticsProvider... analyticsProviders) {
        checkArgument(analyticsProviders.length > 0, "Need to provide at least one analytics provider");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(Settings.ANALYTICS, true);
        mCloudPlaybackStateWrapper = cloudPlaybackStateWrapper;
    }

    /**
     * Opens an analytics session for activities
     */
    public void openSessionForActivity() {
        sActivitySessionOpen.set(true);
        openSessionIfAnalyticsEnabled();
    }

    /**
     * Closes a analytics session for activities
     */
    public void closeSessionForActivity() {
        sActivitySessionOpen.set(false);
        if (mCloudPlaybackStateWrapper.isPlayerPlaying() || !closeSessionIfAnalyticsEnabled()) {
            Log.d(TAG, "Didn't close analytics session");
        }
    }

    /**
     * Opens an analytics session for the player
     */
    public void openSessionForPlayer() {
        openSessionIfAnalyticsEnabled();
    }

    /**
     * Closes a analytics session for the player
     */
    public void closeSessionForPlayer() {
        if (activitySessionIsClosed()){
            if (closeSessionIfAnalyticsEnabled()) {
                return;
            }
        }
        Log.d(TAG, "Didn't close analytics session for player");
    }

    @VisibleForTesting
    protected boolean activitySessionIsClosed() {
        return !sActivitySessionOpen.get();
    }

    private void openSessionIfAnalyticsEnabled() {
        if (analyticsIsEnabled()) {
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.openSession();
            }
            Log.d(TAG, "Opening analytics session");
        } else {
            Log.d(TAG, "Didn't open analytics session");
        }
    }

    private boolean closeSessionIfAnalyticsEnabled() {
        if (analyticsIsEnabled()) {
            closeSession();
            return true;
        }

        return false;

    }

    private void closeSession() {
        Log.d(TAG, "Closing Analytics Session");
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            analyticsProvider.closeSession();
        }
    }

    private boolean analyticsIsEnabled() {
        return !mAnalyticsProperties.isAnalyticsDisabled() && mAnalyticsPreferenceEnabled;

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.ANALYTICS.equalsIgnoreCase(key)) {
            mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(Settings.ANALYTICS, true);

            if (!mAnalyticsPreferenceEnabled){
                closeSession();
            }
        }
    }

    @VisibleForTesting
    protected boolean isAnalyticsPreferenceEnabled() {
        return mAnalyticsPreferenceEnabled;
    }

    //To make testing easier
    protected static class CloudPlayerStateWrapper {
        public boolean isPlayerPlaying() {
            State playbackState = CloudPlaybackService.getPlaybackState();
            return playbackState.isSupposedToBePlaying();
        }
    }
}
