package com.soundcloud.android.analytics;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackState;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.rx.Event;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
import rx.Subscription;

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
    private Subscription mEnterScreenEventSub;

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
        mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true);
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

    /**
     * Tracks a single screen (Activity or Fragment) under the given tag
     */
    public void trackScreen(String screenTag) {
        if (analyticsIsEnabled() && sActivitySessionOpen.get()) {
            Log.d(TAG, "Track screen " + screenTag);
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.trackScreen(screenTag);
            }
        }
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
            if (mEnterScreenEventSub == null) {
                Log.d(TAG, "Subscribing to SCREEN_ENTERED events");
                mEnterScreenEventSub = Event.SCREEN_ENTERED.subscribe(new ScreenTrackingObserver());
            }
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
        if (mEnterScreenEventSub != null) {
            Log.d(TAG, "Unsubscribing from SCREEN_ENTERED events");
            mEnterScreenEventSub.unsubscribe();
            mEnterScreenEventSub = null;
        }
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            analyticsProvider.closeSession();
        }
    }

    private boolean analyticsIsEnabled() {
        return !mAnalyticsProperties.isAnalyticsDisabled() && mAnalyticsPreferenceEnabled;

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.ANALYTICS_ENABLED.equalsIgnoreCase(key)) {
            mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true);

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
            PlaybackState playbackPlaybackState = PlaybackService.getPlaybackState();
            return playbackPlaybackState.isSupposedToBePlaying();
        }
    }

    private final class ScreenTrackingObserver extends DefaultObserver<String> {
        @Override
        public void onNext(String screenTag) {
            //TODO Be defensive, check screenTag value
            //If dev/beta build and empty crash the app, otherwise log silent error
            Log.d(TAG, "ScreenTrackingObserver onNext: " + screenTag);
            trackScreen(screenTag);
        }
    }
}
