package com.soundcloud.android.analytics;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackState;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
import rx.Scheduler;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The engine which drives sending analytics. Important that all analytics providers to this engine
 * do not rely on singletons to do their work. It should be possible to create multiple providers and open sessions,
 * close sessions and handle the events being sent in a multi-threaded environment.
 */
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

    @VisibleForTesting
    static final AtomicBoolean ACTIVITY_SESSION_OPEN = new AtomicBoolean();
    @VisibleForTesting
    static final long FLUSH_DELAY_SECONDS = 120L;

    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;
    private PlaybackServiceStateWrapper mPlaybackStateWrapper;

    private CompositeSubscription mEventsSubscription;
    private BooleanSubscription mFlushSubscription;
    private Scheduler mScheduler;

    private boolean mAnalyticsEnabled;

    // will be called by the Rx scheduler after a given delay, as long as events come in
    private final Action0 mFlushAction = new Action0() {
        @Override
        public void call() {
            Log.d(AnalyticsEngine.this, "Flushing event data");
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.flush();
            }
            mFlushSubscription.unsubscribe();
        }
    };

    public AnalyticsEngine(Context context, SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties) {
        this(sharedPreferences, analyticsProperties,
                new PlaybackServiceStateWrapper(), AndroidSchedulers.mainThread(),
                new LocalyticsAnalyticsProvider(context, analyticsProperties),
                new EventLoggerAnalyticsProvider(),
                new ComScoreAnalyticsProvider(context));
    }

    @VisibleForTesting
    protected AnalyticsEngine(SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties,
                              PlaybackServiceStateWrapper playbackStateWrapper,
                              Scheduler scheduler, AnalyticsProvider... analyticsProviders) {
        Log.d(this, "Creating analytics engine");
        checkArgument(analyticsProviders.length > 0, "Need to provide at least one analytics provider");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
        mPlaybackStateWrapper = playbackStateWrapper;
        mScheduler = scheduler;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        handleAnalyticsAvailability(sharedPreferences);
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.ANALYTICS_ENABLED.equals(key)) {
            handleAnalyticsAvailability(sharedPreferences);
        }
    }

    private void handleAnalyticsAvailability(SharedPreferences sharedPreferences) {
        // TODO: as long as the playback service makes direct calls into this class (which it shouldn't), we'll have to
        // cache this to be used as guards in the open/closeSessionForPlayer methods
        unsubscribeFromEvents();
        mAnalyticsEnabled = sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true);
        if (mAnalyticsProperties.isAnalyticsAvailable() && mAnalyticsEnabled) {
            Log.d(this, "Subscribing to events");
            mEventsSubscription = new CompositeSubscription();
            mEventsSubscription.add(EventBus.PLAYBACK.subscribe(new PlaybackEventObserver()));
            mEventsSubscription.add(EventBus.SOCIAL.subscribe(new SocialEventObserver()));
            mEventsSubscription.add(EventBus.ACTIVITY_LIFECYCLE.subscribe(new ActivityEventObserver()));
            mEventsSubscription.add(EventBus.SCREEN_ENTERED.subscribe(new ScreenTrackingObserver()));
        }
    }

    @VisibleForTesting
    void unsubscribeFromEvents() {
        Log.d(this, "Unsubscribing from events");
        if (mEventsSubscription != null) {
            mEventsSubscription.unsubscribe();
        }
    }

    private void scheduleFlush() {
        if (mFlushSubscription == null || mFlushSubscription.isUnsubscribed()) {
            Log.d(this, "Scheduling flush in " + FLUSH_DELAY_SECONDS + " secs");
            final Subscription subscription = mScheduler.schedule(mFlushAction, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS);
            // FIXME: Clunky. Replace with a mutable SerialSubscription once we update RxJava to 0.16.+
            mFlushSubscription = new BooleanSubscription() {
                @Override
                public void unsubscribe() {
                    subscription.unsubscribe();
                    super.unsubscribe();
                }
            };
        } else {
            Log.d(this, "Ignoring flush event; already scheduled");
        }
    }

    /**
     * Opens an analytics session for activities
     */
    private void openSessionForActivity() {
        ACTIVITY_SESSION_OPEN.set(true);
        openSession();
    }

    /**
     * Closes a analytics session for activities
     */
    private void closeSessionForActivity() {
        ACTIVITY_SESSION_OPEN.set(false);
        if (mPlaybackStateWrapper.isPlayerPlaying()) {
            Log.d(this, "Didn't close analytics session; playback service still alive and well!");
        } else {
            closeSession();
        }
    }

    /**
     * Opens an analytics session for the player
     */
    public void openSessionForPlayer() {
        if (mAnalyticsProperties.isAnalyticsAvailable() && mAnalyticsEnabled) {
            openSession();
        }
    }

    /**
     * Closes a analytics session for the player
     */
    public void closeSessionForPlayer() {
        if (mAnalyticsProperties.isAnalyticsAvailable() && mAnalyticsEnabled && isActivitySessionClosed()) {
            closeSession();
        }
        Log.d(this, "Didn't close analytics session for player");
    }

    /**
     * Tracks a single screen (Activity or Fragment) under the given tag
     */
    private void trackScreen(String screenTag) {
        if (ACTIVITY_SESSION_OPEN.get()) {
            Log.d(this, "Track screen " + screenTag);
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                try {
                    analyticsProvider.trackScreen(screenTag);
                } catch (Throwable t) {
                    handleProviderError(t, analyticsProvider, "trackScreen");
                }
            }
        }
    }

    /**
     * Tracks a playback event.
     *
     * This currently will get tracked regardless of Sessions or Analytics settings. Need to make sure this
     * should be the case for all providers, not just {@link EventLoggerAnalyticsProvider}
     */
    public void trackPlaybackEvent(PlaybackEvent playbackEvent) {
        Log.d(this, "Track playback event " + playbackEvent);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.trackPlaybackEvent(playbackEvent);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "trackPlaybackEvent");
            }
        }
    }

    /**
     * Tracks a social engagement event
     */
    public void trackSocialEvent(SocialEvent event) {
        Log.d(this, "Track social event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.trackSocialEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "trackSocialEvent");
            }
        }
    }

    @VisibleForTesting
    protected boolean isActivitySessionClosed() {
        return !ACTIVITY_SESSION_OPEN.get();
    }

    private void openSession() {
        Log.d(this, "Open session");
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.openSession();
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "openSession");
            }
        }
    }

    private void closeSession() {
        Log.d(this, "Close session");
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.closeSession();
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "closeSession");
            }
        }
    }

    private void handleProviderError(Throwable t, AnalyticsProvider provider, String methodName) {
        final String message = String.format("exception while processing %s for provider %s, with error = %s",
                methodName, provider.getClass(), t.toString());
        Log.e(this, message);
        SoundCloudApplication.handleSilentException(message, t);
    }

    //To make testing easier
    protected static class PlaybackServiceStateWrapper {
        public boolean isPlayerPlaying() {
            PlaybackState playbackPlaybackState = PlaybackService.getPlaybackState();
            return playbackPlaybackState.isSupposedToBePlaying();
        }
    }

    private final class ActivityEventObserver extends DefaultObserver<ActivityLifeCycleEvent> {
        @Override
        public void onNext(ActivityLifeCycleEvent event) {
            Log.d(this, "ActivityEventObserver onNext: " + event);
            if (event.isCreateEvent() || event.isResumeEvent()) {
                openSessionForActivity();
            } else {
                closeSessionForActivity();
            }
            scheduleFlush();
        }
    }

    private final class ScreenTrackingObserver extends DefaultObserver<String> {
        @Override
        public void onNext(String screenTag) {
            //TODO Be defensive, check screenTag value
            //If dev/beta build and empty crash the app, otherwise log silent error
            Log.d(this, "ScreenTrackingObserver onNext: " + screenTag);
            trackScreen(screenTag);
            scheduleFlush();
        }
    }

    private final class PlaybackEventObserver extends DefaultObserver<PlaybackEvent> {
        @Override
        public void onNext(PlaybackEvent args) {
            Log.d(this, "PlaybackEventObserver onNext: " + args);
            trackPlaybackEvent(args);
            scheduleFlush();
        }
    }

    private final class SocialEventObserver extends DefaultObserver<SocialEvent> {
        @Override
        public void onNext(SocialEvent args) {
            Log.d(this, "SocialEventObserver onNext: " + args);
            trackSocialEvent(args);
            scheduleFlush();
        }
    }
}
