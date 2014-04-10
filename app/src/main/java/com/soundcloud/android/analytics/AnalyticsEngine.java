package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

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

    private final EventBus mEventBus;
    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;

    private CompositeSubscription mEventsSubscription;
    private SerialSubscription mFlushSubscription = new SerialSubscription();
    private Scheduler mScheduler;

    // will be called by the Rx scheduler after a given delay, as long as events come in
    private final Action1<Scheduler.Inner> mFlushAction = new Action1<Scheduler.Inner>() {
        @Override
        public void call(Scheduler.Inner inner) {
            Log.d(AnalyticsEngine.this, "Flushing event data");
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.flush();
            }
            mFlushSubscription.unsubscribe();
            mFlushSubscription = new SerialSubscription();
        }
    };

    public AnalyticsEngine(EventBus eventBus, SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties,
            List<AnalyticsProvider> analyticsProviders) {
        this(eventBus, sharedPreferences, analyticsProperties, AndroidSchedulers.mainThread(), analyticsProviders);
    }

    @VisibleForTesting
    protected AnalyticsEngine(EventBus eventBus, SharedPreferences sharedPreferences, AnalyticsProperties analyticsProperties,
                              Scheduler scheduler, List<AnalyticsProvider> analyticsProviders) {
        Log.d(this, "Creating analytics engine");
        mEventBus = eventBus;
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
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.PLAYBACK, new PlaybackEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.PLAYBACK_PERFORMANCE, new PlaybackPerformanceEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.UI, new UIEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.ONBOARDING, new OnboardEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, new ActivityEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.SCREEN_ENTERED, new ScreenEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new UserEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.SEARCH, new SearchEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.PLAY_CONTROL, new PlayControlSubscriber()));
        }
    }

    @VisibleForTesting
    void unsubscribeFromEvents() {
        if (mEventsSubscription != null) {
            Log.d(this, "Unsubscribing from events");
            mEventsSubscription.unsubscribe();
        }
    }

    private void scheduleFlush() {
        if (mFlushSubscription.get() == Subscriptions.empty()) {
            Log.d(this, "Scheduling flush in " + FLUSH_DELAY_SECONDS + " secs");
            mFlushSubscription.set(mScheduler.schedule(mFlushAction, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS));
        } else {
            Log.d(this, "Ignoring flush event; already scheduled");
        }
    }

    private void handleProviderError(Throwable t, AnalyticsProvider provider, String methodName) {
        final String message = String.format("exception while processing %s for provider %s, with error = %s",
                methodName, provider.getClass(), t.toString());
        Log.e(this, message);
        SoundCloudApplication.handleSilentException(message, t);
    }

    private final class ActivityEventSubscriber extends EventSubscriber<ActivityLifeCycleEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, ActivityLifeCycleEvent event) {
            provider.handleActivityLifeCycleEvent(event);
        }
    }

    private final class ScreenEventSubscriber extends EventSubscriber<String> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, String event) {
            provider.handleScreenEvent(event);
        }
    }

    private final class UserEventSubscriber extends EventSubscriber<CurrentUserChangedEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, CurrentUserChangedEvent event) {
            provider.handleCurrentUserChangedEvent(event);
        }
    }

    private final class PlaybackEventSubscriber extends EventSubscriber<PlaybackEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PlaybackEvent event) {
            provider.handlePlaybackEvent(event);
        }
    }

    private final class PlaybackPerformanceEventSubscriber extends EventSubscriber<PlaybackPerformanceEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PlaybackPerformanceEvent event) {
            provider.handlePlaybackPerformanceEvent(event);
        }
    }

    private final class UIEventSubscriber extends EventSubscriber<UIEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, UIEvent event) {
            provider.handleUIEvent(event);
        }
    }

    private final class OnboardEventSubscriber extends EventSubscriber<OnboardingEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, OnboardingEvent event) {
            provider.handleOnboardingEvent(event);
        }
    }

    private final class SearchEventSubscriber extends EventSubscriber<SearchEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, SearchEvent event) {
            provider.handleSearchEvent(event);
        }
    }

    private final class PlayControlSubscriber extends EventSubscriber<PlayControlEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, PlayControlEvent event) {
            provider.handlePlayControlEvent(event);
        }
    }

    private abstract class EventSubscriber<EventT> extends DefaultSubscriber<EventT> {
        @Override
        public void onNext(EventT event) {
            Log.d(AnalyticsEngine.this, "Track event " + event);
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
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
