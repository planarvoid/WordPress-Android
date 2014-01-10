package com.soundcloud.android.analytics;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackState;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
import rx.Scheduler;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Action0;

import android.content.Context;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The engine which drives sending analytics. Important that all analytics providers to this engine
 * do not rely on singletons to do their work. It should be possible to create multiple providers and open sessions,
 * close sessions and handle the events being sent in a multi-threaded environment.
 * <p/>
 * The analytics engine should be used in aspects located in the aspect folder under src/main/java
 */
public class AnalyticsEngine {

    @VisibleForTesting
    static final AtomicBoolean ACTIVITY_SESSION_OPEN = new AtomicBoolean();
    @VisibleForTesting
    static long FLUSH_DELAY_SECONDS = 5L; // FIXME: set to realistic value

    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;
    private PlaybackServiceStateWrapper mPlaybackStateWrapper;

    private Subscription mEnterScreenEventSub;
    private BooleanSubscription mFlushSubscription;
    private Scheduler mScheduler;

    // will be called by the Rx scheduler after a given delay, as long as events come in
    private final Action0 mFlushAction = new Action0() {
        @Override
        public void call() {
            if (mAnalyticsProperties.isAnalyticsEnabled()) {
                Log.d(AnalyticsEngine.this, "Flushing event data");
                for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                    analyticsProvider.flush();
                }
                mFlushSubscription.unsubscribe();
            }
        }
    };

    public AnalyticsEngine(Context context, AnalyticsProperties analyticsProperties) {
        this(analyticsProperties,
                new PlaybackServiceStateWrapper(), AndroidSchedulers.mainThread(),
                new LocalyticsAnalyticsProvider(context, analyticsProperties),
                new EventLoggerAnalyticsProvider());
    }

    @VisibleForTesting
    protected AnalyticsEngine(AnalyticsProperties analyticsProperties, PlaybackServiceStateWrapper playbackStateWrapper,
                              Scheduler scheduler, AnalyticsProvider... analyticsProviders) {
        Log.d(this, "Creating analytics engine");
        checkArgument(analyticsProviders.length > 0, "Need to provide at least one analytics provider");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
        mPlaybackStateWrapper = playbackStateWrapper;
        mScheduler = scheduler;

        Event.PLAYBACK.subscribe(new PlaybackEventObserver());
        Event.SOCIAL.subscribe(new SocialEventObserver());
        Event.ACTIVITY_EVENT.subscribe(new ActivityEventObserver());
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
        openSessionIfAnalyticsEnabled();
    }

    /**
     * Closes a analytics session for activities
     */
    private void closeSessionForActivity() {
        ACTIVITY_SESSION_OPEN.set(false);
        if (mPlaybackStateWrapper.isPlayerPlaying() || !closeSessionIfAnalyticsEnabled()) {
            Log.d(this, "Didn't close analytics session");
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
        Log.d(this, "Didn't close analytics session for player");
    }

    /**
     * Tracks a single screen (Activity or Fragment) under the given tag
     */
    private void trackScreen(String screenTag) {
        if (mAnalyticsProperties.isAnalyticsEnabled() && ACTIVITY_SESSION_OPEN.get()) {
            Log.d(this, "Track screen " + screenTag);
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.trackScreen(screenTag);
            }
        }
    }

    /**
     * Tracks a playback event.
     *
     * This currently will get tracked regardless of Sessions or Analytics settings. Need to make sure this
     * should be the case for all providers, not just {@link EventLoggerAnalyticsProvider}
     */
    public void trackPlaybackEvent(PlaybackEventData playbackEventData) {
        Log.d(this, "Track playback event " + playbackEventData);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            analyticsProvider.trackPlaybackEvent(playbackEventData);
        }
    }

    /**
     * Tracks a social engagement event
     */
    public void trackSocialEvent(SocialEvent event) {
        Log.d(this, "Track social event " + event);
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            analyticsProvider.trackSocialEvent(event);
        }
    }

    @VisibleForTesting
    protected boolean activitySessionIsClosed() {
        return !ACTIVITY_SESSION_OPEN.get();
    }

    private void openSessionIfAnalyticsEnabled() {
        if (mAnalyticsProperties.isAnalyticsEnabled()) {
            Log.d(this, "Open session");
            for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
                analyticsProvider.openSession();
            }
            if (mEnterScreenEventSub == null) {
                Log.d(this, "Subscribing to SCREEN_ENTERED events");
                mEnterScreenEventSub = Event.SCREEN_ENTERED.subscribe(new ScreenTrackingObserver());
            }
        }
    }

    private boolean closeSessionIfAnalyticsEnabled() {
        if (mAnalyticsProperties.isAnalyticsEnabled()) {
            closeSession();
            return true;
        }

        return false;

    }

    private void closeSession() {
        if (mEnterScreenEventSub != null) {
            Log.d(this, "Unsubscribing from SCREEN_ENTERED events");
            mEnterScreenEventSub.unsubscribe();
            mEnterScreenEventSub = null;
        }
        for (AnalyticsProvider analyticsProvider : mAnalyticsProviders) {
            analyticsProvider.closeSession();
        }
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

    private final class PlaybackEventObserver extends DefaultObserver<PlaybackEventData> {
        @Override
        public void onNext(PlaybackEventData args) {
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
