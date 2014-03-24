package com.soundcloud.android.events;

import java.util.HashMap;
import java.util.Map;

public class PlayControlEvent {

    public static final String EXTRA_EVENT_SOURCE = "play_event_source";

    public static final String SOURCE_NOTIFICATION = "notification";
    public static final String SOURCE_WIDGET = "widget";
    public static final String SOURCE_REMOTE = "lockscreen";

    private final Map<String, String> mAttributes;

    private PlayControlEvent() {
        mAttributes = new HashMap<String, String>();
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }

    @Override
    public String toString() {
        return  String.format("PlayControlEvent with attributes %s", mAttributes.toString());
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
        mAttributes.put(key, value);
        return this;
    }

}
