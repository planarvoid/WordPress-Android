package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
import rx.Scheduler;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

import android.content.SharedPreferences;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The engine which drives sending analytics. It acts as an event broker which forwards system events relevant for
 * analytics to any number of registered {@link AnalyticsProvider}s, and enables/disabled itself based on both
 * availability of analytics in the current build as well as user toggled application settings.
 */
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

    @VisibleForTesting
    static final long FLUSH_DELAY_SECONDS = 120L;

    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;

    private CompositeSubscription mEventsSubscription;
    private BooleanSubscription mFlushSubscription;
    private Scheduler mScheduler;

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

    public AnalyticsEngine(SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties,
            List<AnalyticsProvider> analyticsProviders) {
        this(sharedPreferences, analyticsProperties, AndroidSchedulers.mainThread(), analyticsProviders);
    }

    @VisibleForTesting
    protected AnalyticsEngine(SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties,
                              Scheduler scheduler, List<AnalyticsProvider> analyticsProviders) {
        Log.d(this, "Creating analytics engine");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
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
        unsubscribeFromEvents();
        boolean analyticsEnabled = sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true);
        if (mAnalyticsProperties.isAnalyticsAvailable() && analyticsEnabled) {
            Log.d(this, "Subscribing to events");
            mEventsSubscription = new CompositeSubscription();
            mEventsSubscription.add(EventBus.PLAYBACK.subscribe(new PlaybackEventObserver()));
            mEventsSubscription.add(EventBus.SOCIAL.subscribe(new SocialEventObserver()));
            mEventsSubscription.add(EventBus.ONBOARDING.subscribe(new OnboardingEventObserver()));
            mEventsSubscription.add(EventBus.ACTIVITY_LIFECYCLE.subscribe(new ActivityEventObserver()));
            mEventsSubscription.add(EventBus.SCREEN_ENTERED.subscribe(new ScreenEventObserver()));
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

    private void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        Log.d(this, "Activity life-cycle event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleActivityLifeCycleEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleActivityLifeCycleEvent");
            }
        }
    }

    private void handleScreenEvent(String screenTag) {
        Log.d(this, "Track screen " + screenTag);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleScreenEvent(screenTag);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleScreenEvent");
            }
        }
    }

    private void handlePlaybackEvent(PlaybackEvent playbackEvent) {
        Log.d(this, "Track playback event " + playbackEvent);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handlePlaybackEvent(playbackEvent);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handlePlaybackEvent");
            }
        }
    }

    private void handleSocialEvent(SocialEvent event) {
        Log.d(this, "Track social event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleSocialEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleSocialEvent");
            }
        }
    }

    private void handleOnboardingEvent(OnboardingEvent event) {
        Log.d(this, "Track onboarding event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleOnboardingEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleOnboardingEvent");
            }
        }
    }

    private void handleProviderError(Throwable t, AnalyticsProvider provider, String methodName) {
        final String message = String.format("exception while processing %s for provider %s, with error = %s",
                methodName, provider.getClass(), t.toString());
        Log.e(this, message);
        SoundCloudApplication.handleSilentException(message, t);
    }

    private final class ActivityEventObserver extends DefaultObserver<ActivityLifeCycleEvent> {
        @Override
        public void onNext(ActivityLifeCycleEvent event) {
            Log.d(this, "ActivityEventObserver onNext: " + event);
            handleActivityLifeCycleEvent(event);
            scheduleFlush();
        }
    }

    private final class ScreenEventObserver extends DefaultObserver<String> {
        @Override
        public void onNext(String screenTag) {
            //TODO Be defensive, check screenTag value
            //If dev/beta build and empty crash the app, otherwise log silent error
            Log.d(this, "ScreenTrackingObserver onNext: " + screenTag);
            handleScreenEvent(screenTag);
            scheduleFlush();
        }
    }

    private final class PlaybackEventObserver extends DefaultObserver<PlaybackEvent> {
        @Override
        public void onNext(PlaybackEvent args) {
            Log.d(this, "PlaybackEventObserver onNext: " + args);
            handlePlaybackEvent(args);
            scheduleFlush();
        }
    }

    private final class SocialEventObserver extends DefaultObserver<SocialEvent> {
        @Override
        public void onNext(SocialEvent args) {
            Log.d(this, "SocialEventObserver onNext: " + args);
            handleSocialEvent(args);
            scheduleFlush();
        }
    }

    private final class OnboardingEventObserver extends DefaultObserver<OnboardingEvent> {
        @Override
        public void onNext(OnboardingEvent args) {
            Log.d(this, "OnboardingEventObserver onNext: " + args);
            handleOnboardingEvent(args);
            scheduleFlush();
        }
    }
}
