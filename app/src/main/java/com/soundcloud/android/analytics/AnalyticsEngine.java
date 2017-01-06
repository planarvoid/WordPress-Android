package com.soundcloud.android.analytics;

import static android.util.Log.INFO;
import static com.soundcloud.android.onboarding.OnboardActivity.ONBOARDING_TAG;
import static com.soundcloud.android.storage.StorageModule.ANALYTICS_SETTINGS;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The engine which drives sending analytics. It acts as an event broker which forwards system events relevant for
 * analytics to any number of registered {@link AnalyticsProvider}s, and enables/disabled itself based on both
 * availability of analytics in the current build as well as user toggled application settings.
 */
@Singleton
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

    @VisibleForTesting
    static final long FLUSH_DELAY_SECONDS = 60L;

    private final EventBus eventBus;
    private final AnalyticsProviderFactory analyticsProviderFactory;
    private final Scheduler scheduler;
    private final AtomicBoolean pendingFlush = new AtomicBoolean(true);

    private Collection<AnalyticsProvider> analyticsProviders;

    // will be called by the Rx scheduler after a given delay, as long as events come in
    private final Action0 flushAction = new Action0() {
        @Override
        public void call() {
            Log.d(AnalyticsEngine.this, "Flushing event data");
            for (AnalyticsProvider analyticsProvider : analyticsProviders) {
                analyticsProvider.flush();
            }
            pendingFlush.set(true);
        }
    };

    @Inject
    AnalyticsEngine(EventBus eventBus,
                    SharedPreferences sharedPreferences,
                    @Named(ANALYTICS_SETTINGS) SharedPreferences analyticsSettings,
                    AnalyticsProviderFactory analyticsProviderFactory) {
        this(eventBus, sharedPreferences, analyticsSettings, AndroidSchedulers.mainThread(), analyticsProviderFactory);
    }

    public void onAppCreated(Context context) {
        for (AnalyticsProvider analyticsProvider : analyticsProviders) {
            analyticsProvider.onAppCreated(context);
        }
    }

    @VisibleForTesting
    AnalyticsEngine(EventBus eventBus,
                    SharedPreferences sharedPreferences,
                    SharedPreferences analyticsSettings,
                    Scheduler scheduler,
                    AnalyticsProviderFactory analyticsProviderFactory) {
        Log.i(this, "Creating analytics engine");
        this.analyticsProviderFactory = analyticsProviderFactory;
        this.analyticsProviders = analyticsProviderFactory.getProviders();
        this.eventBus = eventBus;
        this.scheduler = scheduler;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        analyticsSettings.registerOnSharedPreferenceChangeListener(this);
        subscribeEventQueues();
    }

    private void subscribeEventQueues() {
        Log.d(this, "Subscribing to events");
        CompositeSubscription eventsSubscription = new CompositeSubscription();
        eventsSubscription.add(eventBus.subscribe(EventQueue.TRACKING, new TrackingEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_PERFORMANCE,
                new PlaybackPerformanceEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_ERROR, new PlaybackErrorEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.ONBOARDING, new OnboardEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, new ActivityEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new UserEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.PERFORMANCE, new PerformanceEventSubscriber()));
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingKey.ANALYTICS_ENABLED.equals(key) || AnalyticsProviderFactory.DISABLED_PROVIDERS.equals(key)) {
            analyticsProviders = analyticsProviderFactory.getProviders();
        }

    }

    private void scheduleFlush() {
        if (pendingFlush.getAndSet(false)) {
            Log.d(this, "Scheduling flush in " + FLUSH_DELAY_SECONDS + " secs");
            scheduler.createWorker().schedule(flushAction, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            Log.d(this, "Ignoring flush event; already scheduled");
        }
    }

    private void handleProviderError(Throwable t, AnalyticsProvider provider, String methodName) {
        final String message = String.format("exception while processing %s for provider %s, with error = %s",
                methodName, provider.getClass(), t.toString());
        Log.e(this, message);
        ErrorUtils.handleSilentException(message, t);
    }

    private final class ActivityEventSubscriber extends EventSubscriber<ActivityLifeCycleEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, ActivityLifeCycleEvent event) {
            provider.handleActivityLifeCycleEvent(event);
        }
    }

    private final class UserEventSubscriber extends EventSubscriber<CurrentUserChangedEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, CurrentUserChangedEvent event) {
            provider.handleCurrentUserChangedEvent(event);
        }
    }

    private final class PlaybackPerformanceEventSubscriber extends EventSubscriber<PlaybackPerformanceEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PlaybackPerformanceEvent event) {
            provider.handlePlaybackPerformanceEvent(event);
        }
    }

    private final class PlaybackErrorEventSubscriber extends EventSubscriber<PlaybackErrorEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PlaybackErrorEvent event) {
            provider.handlePlaybackErrorEvent(event);
        }
    }

    private final class OnboardEventSubscriber extends EventSubscriber<OnboardingEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, OnboardingEvent event) {
            provider.handleOnboardingEvent(event);
        }
    }

    private final class TrackingEventSubscriber extends EventSubscriber<TrackingEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, TrackingEvent event) {
            provider.handleTrackingEvent(event);
        }
    }

    private final class PerformanceEventSubscriber extends EventSubscriber<PerformanceEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PerformanceEvent event) {
            provider.handlePerformanceEvent(event);
        }
    }

    private abstract class EventSubscriber<EventT> extends DefaultSubscriber<EventT> {
        @Override
        public void onNext(EventT event) {
            Log.d(AnalyticsEngine.this, "Track event " + event);
            if (event instanceof OnboardingEvent) {
                log(INFO, ONBOARDING_TAG, "onboarding event published: " + event);
            }

            for (AnalyticsProvider analyticsProvider : analyticsProviders) {
                try {
                    handleEvent(analyticsProvider, event);
                } catch (Throwable t) {
                    handleProviderError(t, analyticsProvider, getClass().getSimpleName());
                }
            }
            scheduleFlush();
        }

        protected abstract void handleEvent(AnalyticsProvider provider, EventT event);
    }
}
