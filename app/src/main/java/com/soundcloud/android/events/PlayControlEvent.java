package com.soundcloud.android.events;

import com.soundcloud.android.Consts;

public final class PlayControlEvent extends TrackingEvent {

    public static final String EXTRA_EVENT_SOURCE = "play_event_source";

    public static final String SOURCE_NOTIFICATION = "notification";
    public static final String SOURCE_WIDGET = "widget";
    public static final String SOURCE_REMOTE = "lockscreen";
    public static final String SOURCE_FOOTER_PLAYER = "footer_player";
    public static final String SOURCE_FULL_PLAYER = "full_player";

    private static final String ATTRIBUTE_LOCATION = "location";
    private static final String ATTRIBUTE_ACTION = "action";
    private static final String ATTRIBUTE_TAP_OR_SWIPE = "tap or swipe";

    private static final String ACTION_SKIP = "skip";
    private static final String ACTION_PREV = "prev";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_PLAY = "play";
    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_SCRUB = "scrub";
    private static final String SKIP_TYPE_SWIPE = "swipe";
    private static final String SKIP_TYPE_TAP = "tap";

    protected PlayControlEvent() {
        super(KIND_DEFAULT, Consts.NOT_SET);
    }

    @Override
    public String toString() {
        return String.format("PlayControlEvent with attributes %s", attributes.toString());
    }

    public static PlayControlEvent swipePrevious(boolean isExpanded) {
        return swipe(isExpanded, false);
    }

    public static PlayControlEvent swipeSkip(boolean isExpanded) {
        return swipe(isExpanded, true);
    }

    private static PlayControlEvent swipe(boolean isExpanded, boolean isSkip) {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, isSkip ? ACTION_SKIP : ACTION_PREV)
                .put(ATTRIBUTE_TAP_OR_SWIPE, SKIP_TYPE_SWIPE)
                .put(ATTRIBUTE_LOCATION, getSourcePlayerFrom(isExpanded));
    }

    public static PlayControlEvent skipAd() {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, "skip_ad_button")
                .put(ATTRIBUTE_TAP_OR_SWIPE, SKIP_TYPE_TAP)
                .put(ATTRIBUTE_LOCATION, SOURCE_FULL_PLAYER);
    }

    private static String getSourcePlayerFrom(boolean isExpanded) {
        return isExpanded ? SOURCE_FULL_PLAYER : SOURCE_FOOTER_PLAYER;
    }

    public static PlayControlEvent toggle(String source, boolean isPlaying) {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, isPlaying ? ACTION_PAUSE : ACTION_PLAY)
                .put(ATTRIBUTE_TAP_OR_SWIPE, SKIP_TYPE_TAP)
                .put(ATTRIBUTE_LOCATION, source);
    }

    public static PlayControlEvent close(String source) {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, ACTION_CLOSE)
                .put(ATTRIBUTE_TAP_OR_SWIPE, SKIP_TYPE_TAP)
                .put(ATTRIBUTE_LOCATION, source);
    }

    public static PlayControlEvent play(String source) {
        return toggle(source, false);
    }

    public static PlayControlEvent pause(String source) {
        return toggle(source, true);
    }

    public static PlayControlEvent skip(String source) {
        return tap(source, true);
    }

    public static PlayControlEvent previous(String source) {
        return tap(source, false);
    }

    private static PlayControlEvent tap(String source, boolean isSkip) {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, isSkip ? ACTION_SKIP : ACTION_PREV)
                .put(ATTRIBUTE_TAP_OR_SWIPE, SKIP_TYPE_TAP)
                .put(ATTRIBUTE_LOCATION, source);
    }

    public static PlayControlEvent scrub(String source) {
        return new PlayControlEvent()
                .put(ATTRIBUTE_ACTION, ACTION_SCRUB)
                .put(ATTRIBUTE_LOCATION, source);
    }

}
