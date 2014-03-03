package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
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
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.UI, new UIEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.ONBOARDING, new OnboardingEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, new ActivityEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.SCREEN_ENTERED, new ScreenEventSubscriber()));
            mEventsSubscription.add(mEventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new UserChangeEventSubscriber()));
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

    private void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
        Log.d(this, "User changed event " + event.getCurrentUser());
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleCurrentUserChangedEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleCurrentUserChangedEvent");
            }
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

    private void handleUIEvent(UIEvent event) {
        Log.d(this, "Track UI event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            try {
                analyticsProvider.handleUIEvent(event);
            } catch (Throwable t) {
                handleProviderError(t, analyticsProvider, "handleUIEvent");
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

    private final class UserChangeEventSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        @Override
        public void onNext(CurrentUserChangedEvent event) {
            Log.d(this, "UserChangeEventObserver onNext: " + event);
            handleCurrentUserChangedEvent(event);
            scheduleFlush();
        }
    }

    private final class ActivityEventSubscriber extends DefaultSubscriber<ActivityLifeCycleEvent> {
        @Override
        public void onNext(ActivityLifeCycleEvent event) {
            Log.d(this, "ActivityEventObserver onNext: " + event);
            handleActivityLifeCycleEvent(event);
            scheduleFlush();
        }
    }

    private final class ScreenEventSubscriber extends DefaultSubscriber<String> {
        @Override
        public void onNext(String screenTag) {
            //TODO Be defensive, check screenTag value
            //If dev/beta build and empty crash the app, otherwise log silent error
            Log.d(this, "ScreenTrackingObserver onNext: " + screenTag);
            handleScreenEvent(screenTag);
            scheduleFlush();
        }
    }

    private final class PlaybackEventSubscriber extends DefaultSubscriber<PlaybackEvent> {
        @Override
        public void onNext(PlaybackEvent args) {
            Log.d(this, "PlaybackEventObserver onNext: " + args);
            handlePlaybackEvent(args);
            scheduleFlush();
        }
    }

    private final class UIEventSubscriber extends DefaultSubscriber<UIEvent> {
        @Override
        public void onNext(UIEvent args) {
            Log.d(this, "UIEventObserver onNext: " + args);
            handleUIEvent(args);
            scheduleFlush();
        }
    }

    private final class OnboardingEventSubscriber extends DefaultSubscriber<OnboardingEvent> {
        @Override
        public void onNext(OnboardingEvent args) {
            Log.d(this, "OnboardingEventObserver onNext: " + args);
            handleOnboardingEvent(args);
            scheduleFlush();
        }
    }
}
