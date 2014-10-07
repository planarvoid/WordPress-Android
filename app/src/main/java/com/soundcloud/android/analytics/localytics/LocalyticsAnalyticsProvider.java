package com.soundcloud.android.analytics.localytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class LocalyticsAnalyticsProvider implements AnalyticsProvider {

    public static final String TAG = "LocalyticsProvider";

    @VisibleForTesting
    static final AtomicBoolean ACTIVITY_SESSION_OPEN = new AtomicBoolean();

    private static final int NO_USER = -1;
    private static final long SESSION_EXPIRY = TimeUnit.MINUTES.toMillis(1);

    private final LocalyticsSession session;
    private final LocalyticsUIEventHandler uiEventHandler;
    private final LocalyticsOnboardingEventHandler onboardingEventHandler;
    private final PlaybackStateProvider playbackStateWrapper;
    private final LocalyticsSearchEventHandler searchEventHandler;

    static {
        LocalyticsSession.setSessionExpiration(SESSION_EXPIRY);
    }

    @Inject
    public LocalyticsAnalyticsProvider(Context context, AnalyticsProperties analyticsProperties, AccountOperations accountOperations) {
        this(new LocalyticsSession(context.getApplicationContext(), analyticsProperties.getLocalyticsAppKey()),
                new PlaybackStateProvider(), accountOperations.getLoggedInUserId());
    }

    @VisibleForTesting
    protected LocalyticsAnalyticsProvider(LocalyticsSession localyticsSession,
                                          PlaybackStateProvider playbackStateWrapper, long currentUserId) {
        this(localyticsSession, playbackStateWrapper);
        localyticsSession.setCustomerId(getCustomerId(currentUserId));
    }

    @VisibleForTesting
    protected LocalyticsAnalyticsProvider(LocalyticsSession localyticsSession,
                                          PlaybackStateProvider playbackStateWrapper) {
        session = localyticsSession;
        uiEventHandler = new LocalyticsUIEventHandler(session);
        onboardingEventHandler = new LocalyticsOnboardingEventHandler(session);
        searchEventHandler = new LocalyticsSearchEventHandler(session);
        this.playbackStateWrapper = playbackStateWrapper;
    }

    @Override
    public void flush() {
        session.upload();
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
        int eventKind = event.getKind();
        if (eventKind == CurrentUserChangedEvent.USER_UPDATED) {
            session.setCustomerId(Long.toString(event.getCurrentUser().getId()));
        } else if (eventKind == CurrentUserChangedEvent.USER_REMOVED) {
            session.setCustomerId(null);
        }
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        switch (event.getKind()) {
            case ActivityLifeCycleEvent.ON_CREATE_EVENT:
            case ActivityLifeCycleEvent.ON_RESUME_EVENT:
                openSessionForActivity();
                break;
            case ActivityLifeCycleEvent.ON_PAUSE_EVENT:
                closeSessionForActivity();
                break;
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    public void handlePlayControlEvent(PlayControlEvent event) {
        tagEvent(LocalyticsEvents.PLAY_CONTROLS, event.getAttributes());
    }

    private void tagEvent(String tagName, Map<String, String> attributes) {
        logAttributes(tagName, attributes);
        session.tagEvent(tagName, attributes);
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
        onboardingEventHandler.handleEvent(event);
    }

    @Override
    public void handleSearchEvent(SearchEvent event) {
        searchEventHandler.handleEvent(event);
    }

    @Override
    public void handleAudioAdCompanionImpression(AudioAdCompanionImpressionEvent event) {}

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PlaybackSessionEvent) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        } else if (event instanceof UIEvent) {
            uiEventHandler.handleEvent((UIEvent) event);
        } else if (event instanceof ScreenEvent) {
            handleScreenEvent(event);
        }
    }

    private void handleScreenEvent(TrackingEvent event) {
        final String screenTag = event.get(ScreenEvent.KEY_SCREEN);
        session.tagScreen(screenTag);
        Map<String, String> eventAttributes = new ArrayMap<>();
        eventAttributes.put("context", screenTag);
        tagEvent(LocalyticsEvents.PAGEVIEW, eventAttributes);
    }

    private void handlePlaybackSessionEvent(PlaybackSessionEvent eventData) {
        if (eventData.isStopEvent()) {
            openSession();

            Map<String, String> eventAttributes = new HashMap<String, String>();
            eventAttributes.put("context", eventData.getTrackSourceInfo().getOriginScreen());
            eventAttributes.put("track_id", String.valueOf(new Urn(eventData.get(PlaybackSessionEvent.KEY_TRACK_URN)).getNumericId()));

            final long duration = eventData.getDuration();
            eventAttributes.put("track_length_ms", String.valueOf(duration));
            eventAttributes.put("track_length_bucket", getTrackLengthBucket(duration));

            eventAttributes.put("play_duration_ms", String.valueOf(eventData.getListenTime()));
            eventAttributes.put("percent_listened", getPercentListenedBucket(eventData, duration));

            if (eventData.getTrackSourceInfo().isFromPlaylist()) {
                eventAttributes.put("set_id", String.valueOf(eventData.getTrackSourceInfo().getPlaylistUrn().getNumericId()));
                eventAttributes.put("set_owner", eventData.isPlayingOwnPlaylist() ? "you" : "other");
            } else {
                eventAttributes.put("set_owner", "none");
            }
            eventAttributes.put("stop_reason", getStopReason(eventData));

            tagEvent(LocalyticsEvents.LISTEN, eventAttributes);
        }
    }

    @VisibleForTesting
    protected boolean isActivitySessionClosed() {
        return !ACTIVITY_SESSION_OPEN.get();
    }

    private void openSession() {
        Log.d(TAG, "opening session");
        session.open();
    }

    private void closeSession() {
        Log.d(TAG, "closing session");
        session.close();
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
        if (playbackStateWrapper.isSupposedToBePlaying()) {
            Log.d(TAG, "Didn't close analytics session; playback service still alive and well!");
        } else {
            closeSession();
        }
    }

    //TODO: Should be a standardized anonymous id
    private String getCustomerId(long currentUserId) {
        if (currentUserId == NO_USER) {
            return null;
        } else {
            return Long.toString(currentUserId);
        }
    }

    private void logAttributes(String tagName, Map<String, String> eventAttributes) {
        if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
            final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(tagName + " with EventAttributes");
            for (String key : eventAttributes.keySet()) {
                toStringHelper.add(key, eventAttributes.get(key));
            }
            Log.d(TAG, toStringHelper.toString());
        }
    }

    private String getPercentListenedBucket(PlaybackSessionEvent eventData, long duration) {
        double percentListened = ((double) eventData.getListenTime()) / duration;
        if (percentListened < .05) {
            return "<5%";
        } else if (percentListened <= .25) {
            return "5% to 25%";
        } else if (percentListened <= .75) {
            return "25% to 75%";
        } else {
            return ">75%";
        }
    }

    private String getTrackLengthBucket(long duration) {
        if (duration < 60 * 1000) {
            return "<1min";
        } else if (duration <= 10 * 60 * 1000) {
            return "1min to 10min";
        } else if (duration <= 30 * 60 * 1000) {
            return "10min to 30min";
        } else if (duration <= 60 * 60 * 1000) {
            return "30min to 1hr";
        } else {
            return ">1hr";
        }
    }

    private String getStopReason(PlaybackSessionEvent eventData) {
        switch (eventData.getStopReason()) {
            case PlaybackSessionEvent.STOP_REASON_PAUSE:
                return "pause";
            case PlaybackSessionEvent.STOP_REASON_BUFFERING:
                return "buffering";
            case PlaybackSessionEvent.STOP_REASON_SKIP:
                return "skip";
            case PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED:
                return "track_finished";
            case PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE:
                return "end_of_content";
            case PlaybackSessionEvent.STOP_REASON_NEW_QUEUE:
                return "context_change";
            case PlaybackSessionEvent.STOP_REASON_ERROR:
                return "playback_error";
            default:
                throw new IllegalArgumentException("Unexpected stop reason : " + eventData.getStopReason());
        }
    }
}
