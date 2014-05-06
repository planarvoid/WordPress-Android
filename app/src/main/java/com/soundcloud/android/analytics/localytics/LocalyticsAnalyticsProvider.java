package com.soundcloud.android.analytics.localytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class LocalyticsAnalyticsProvider implements AnalyticsProvider {

    public static final String TAG = "LocalyticsProvider";

    @VisibleForTesting
    static final AtomicBoolean ACTIVITY_SESSION_OPEN = new AtomicBoolean();

    private static final int NO_USER = -1;

    private final LocalyticsSession session;
    private final LocalyticsUIEventHandler uiEventHandler;
    private final LocalyticsOnboardingEventHandler onboardingEventHandler;
    private final PlaybackStateProvider playbackStateWrapper;
    private final LocalyticsSearchEventHandler searchEventHandler;

    public LocalyticsAnalyticsProvider(Context context, AnalyticsProperties analyticsProperties, long currentUserId) {
        this(new LocalyticsSession(context.getApplicationContext(), analyticsProperties.getLocalyticsAppKey()),
                new PlaybackStateProvider(), currentUserId);
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
    public void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event) {
        if (event.getKind() == PlayerLifeCycleEvent.STATE_IDLE) {
            closeSessionForPlayer();
        }
    }

    @Override
    public void handleScreenEvent(String screenTag) {
        session.tagScreen(screenTag);
        Map<String, String> eventAttributes = new HashMap<String, String>();
        eventAttributes.put("context", screenTag);
        session.tagEvent(LocalyticsEvents.PAGEVIEW, eventAttributes);
    }

    @Override
    public void handlePlaybackSessionEvent(PlaybackSessionEvent eventData) {
        if (eventData.isStopEvent()) {
            openSession();

            Map<String, String> eventAttributes = new HashMap<String, String>();
            eventAttributes.put("context", eventData.getTrackSourceInfo().getOriginScreen());
            eventAttributes.put("track_id", String.valueOf(eventData.getTrack().getId()));

            final int duration = eventData.getTrack().duration;
            eventAttributes.put("track_length_ms", String.valueOf(duration));
            eventAttributes.put("track_length_bucket", getTrackLengthBucket(duration));

            eventAttributes.put("play_duration_ms", String.valueOf(eventData.getListenTime()));
            eventAttributes.put("percent_listened", getPercentListenedBucket(eventData, duration));

            // be careful of null values allowed in attributes, will propogate a hard to trace exception
            final String genreOrTag = eventData.getTrack().getGenreOrTag();
            if (ScTextUtils.isNotBlank(genreOrTag)) {
                eventAttributes.put("tag", genreOrTag);
            }

            if (eventData.getTrackSourceInfo().isFromPlaylist()) {
                eventAttributes.put("set_id", String.valueOf(eventData.getTrackSourceInfo().getPlaylistId()));
                eventAttributes.put("set_owner", eventData.isPlayingOwnPlaylist() ? "you" : "other");
            } else {
                eventAttributes.put("set_owner", "none");
            }
            eventAttributes.put("stop_reason", getStopReason(eventData));

            if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
                logAttributes(eventAttributes);
            }

            session.tagEvent(LocalyticsEvents.LISTEN, eventAttributes);
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    public void handlePlayControlEvent(PlayControlEvent event) {
        session.tagEvent(LocalyticsEvents.PLAY_CONTROLS, event.getAttributes());
    }

    @Override
    public void handleUIEvent(UIEvent event) {
        uiEventHandler.handleEvent(event);
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
        onboardingEventHandler.handleEvent(event);
    }

    @Override
    public void handleSearchEvent(SearchEvent event) {
        searchEventHandler.handleEvent(event);
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

    /**
     * Closes a analytics session for the player
     */
    public void closeSessionForPlayer() {
        if (isActivitySessionClosed()) {
            closeSession();
        } else {
            Log.d(TAG, "Didn't close analytics session for player; activity session still alive and well!");
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

    private void logAttributes(Map<String, String> eventAttributes) {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper("EventAttributes");
        for (String key : eventAttributes.keySet()) {
            toStringHelper.add(key, eventAttributes.get(key));
        }
        Log.d(TAG, toStringHelper.toString());
    }

    private String getPercentListenedBucket(PlaybackSessionEvent eventData, int duration) {
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

    private String getTrackLengthBucket(int duration) {
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
