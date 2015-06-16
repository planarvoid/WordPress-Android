package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static android.util.Log.INFO;
import static com.soundcloud.android.utils.ErrorUtils.log;
import static com.soundcloud.android.utils.Log.ONBOARDING_TAG;

/**
 * The engine which drives sending analytics. It acts as an event broker which forwards system events relevant for
 * analytics to any number of registered {@link AnalyticsProvider}s, and enables/disabled itself based on both
 * availability of analytics in the current build as well as user toggled application settings.
 */
@Singleton
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

    @VisibleForTesting
    static final long FLUSH_DELAY_SECONDS = 120L;

    private final EventBus eventBus;
    private final AnalyticsProviderFactory analyticsProviderFactory;
    private final Scheduler scheduler;

    private Collection<AnalyticsProvider> analyticsProviders;
    private SerialSubscription flushSubscription = new SerialSubscription();

    // will be called by the Rx scheduler after a given delay, as long as events come in
    private final Action0 flushAction = new Action0() {
        @Override
        public void call() {
            Log.d(AnalyticsEngine.this, "Flushing event data");
            for (AnalyticsProvider analyticsProvider : analyticsProviders) {
                analyticsProvider.flush();
            }
            flushSubscription.unsubscribe();
            flushSubscription = new SerialSubscription();
        }
    };

    private final Func1<TrackingEvent, Boolean> isPlaybackSessionEvent = new Func1<TrackingEvent, Boolean>() {
        @Override
        public Boolean call(TrackingEvent trackingEvent) {
            return trackingEvent instanceof PlaybackSessionEvent;
        }
    };

    private final Func1<TrackingEvent, Boolean> toPlaybackSessionStatus = new Func1<TrackingEvent, Boolean>() {
        @Override
        public Boolean call(TrackingEvent trackingEvent) {
            final PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) trackingEvent;
            return playbackSessionEvent.isPlayEvent() || playbackSessionEvent.isBufferingEvent();
        }
    };

    private final Func2<ActivityLifeCycleEvent, Boolean, UserSessionEvent> combineToSessionEvent = new Func2<ActivityLifeCycleEvent, Boolean, UserSessionEvent>() {
        @Override
        public UserSessionEvent call(ActivityLifeCycleEvent activityLifeCycleEvent, Boolean isPlaying) {
            final int activityLifeCycleEventKind = activityLifeCycleEvent.getKind();
            final boolean isForeground = activityLifeCycleEventKind == ActivityLifeCycleEvent.ON_RESUME_EVENT || activityLifeCycleEventKind == ActivityLifeCycleEvent.ON_CREATE_EVENT;

            return isForeground || isPlaying ? UserSessionEvent.OPENED : UserSessionEvent.CLOSED;
        }
    };

    @Inject
    public AnalyticsEngine(EventBus eventBus, SharedPreferences sharedPreferences,
                           AnalyticsProviderFactory analyticsProviderFactory) {
        this(eventBus, sharedPreferences, AndroidSchedulers.mainThread(),
                analyticsProviderFactory);
    }

    @VisibleForTesting
    protected AnalyticsEngine(EventBus eventBus, SharedPreferences sharedPreferences,
                              Scheduler scheduler,
                              AnalyticsProviderFactory analyticsProviderFactory) {
        Log.i(this, "Creating analytics engine");
        this.analyticsProviderFactory = analyticsProviderFactory;
        this.analyticsProviders = analyticsProviderFactory.getProviders();
        this.eventBus = eventBus;
        this.scheduler = scheduler;

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        subscribeEventQueues();
    }

    private void subscribeEventQueues() {
        Log.d(this, "Subscribing to events");
        CompositeSubscription eventsSubscription = new CompositeSubscription();
        eventsSubscription.add(eventBus.subscribe(EventQueue.TRACKING, new TrackingEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_PERFORMANCE, new PlaybackPerformanceEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_ERROR, new PlaybackErrorEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.ONBOARDING, new OnboardEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.ACTIVITY_LIFE_CYCLE, new ActivityEventSubscriber()));
        eventsSubscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new UserEventSubscriber()));
        eventsSubscription.add(getUserSessionEvent().subscribe(new UserSessionSubscriber()));
    }

    private Observable<UserSessionEvent> getUserSessionEvent() {
        final Observable<ActivityLifeCycleEvent> activityLifeCycle = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        final Observable<Boolean> playbackSessionEvents = eventBus
                .queue(EventQueue.TRACKING)
                .filter(isPlaybackSessionEvent)
                .map(toPlaybackSessionStatus)
                .startWith(false);

        return Observable
                .combineLatest(activityLifeCycle, playbackSessionEvents, combineToSessionEvent)
                .distinctUntilChanged();
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingKey.ANALYTICS_ENABLED.equals(key)) {
            analyticsProviders = analyticsProviderFactory.getProviders();
        }
    }

    private void scheduleFlush() {
        if (flushSubscription.get() == Subscriptions.empty()) {
            Log.d(this, "Scheduling flush in " + FLUSH_DELAY_SECONDS + " secs");
            flushSubscription.set(scheduler.createWorker().schedule(flushAction, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS));
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

    private class UserSessionSubscriber extends EventSubscriber<UserSessionEvent> {
        @Override
        protected void handleEvent(AnalyticsProvider provider, UserSessionEvent event) {
            provider.handleUserSessionEvent(event);
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
