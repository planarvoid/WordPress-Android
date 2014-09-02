package com.soundcloud.android.events;

import java.util.HashMap;
import java.util.Map;

public final class PlayControlEvent {

    public static final String EXTRA_EVENT_SOURCE = "play_event_source";

    public static final String SOURCE_NOTIFICATION = "notification";
    public static final String SOURCE_WIDGET = "widget";
    public static final String SOURCE_REMOTE = "lockscreen";
    public static final String SOURCE_FOOTER_PLAYER = "footer_player";
    public static final String SOURCE_FULL_PLAYER = "full_player";

    private final Map<String, String> attributes;

    private PlayControlEvent() {
        attributes = new HashMap<String, String>();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return String.format("PlayControlEvent with attributes %s", attributes.toString());
    }

    public static PlayControlEvent playerSwipePrevious() {
        return new PlayControlEvent()
                .putAttribute("action", "prev")
                .putAttribute("click or swipe", "swipe")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent playerSwipeSkip() {
        return new PlayControlEvent()
                .putAttribute("action", "skip")
                .putAttribute("click or swipe", "swipe")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent playerClickPrevious() {
        return new PlayControlEvent()
                .putAttribute("action", "prev")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent playerClickSkip() {
        return new PlayControlEvent()
                .putAttribute("action", "skip")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent playerClickPause() {
        return new PlayControlEvent()
                .putAttribute("action", "pause")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent playerClickPlay() {
        return new PlayControlEvent()
                .putAttribute("action", "play")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", "player");
    }

    public static PlayControlEvent toggle(String source, boolean isPlaying) {
        return new PlayControlEvent()
                .putAttribute("action", isPlaying ? "pause" : "play")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", source);
    }

    public static PlayControlEvent close(String source) {
        return new PlayControlEvent()
                .putAttribute("action", "close")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", source);
    }

    public static PlayControlEvent play(String source) {
        return toggle(source, false);
    }

    public static PlayControlEvent pause(String source) {
        return toggle(source, true);
    }

    public static PlayControlEvent skip(String source) {
        return new PlayControlEvent()
                .putAttribute("action", "skip")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", source);
    }

    public static PlayControlEvent previous(String source) {
        return new PlayControlEvent()
                .putAttribute("action", "prev")
                .putAttribute("click or swipe", "click")
                .putAttribute("location", source);
    }

    private PlayControlEvent putAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayControlEvent that = (PlayControlEvent) o;
        if (!attributes.equals(that.attributes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }
}
