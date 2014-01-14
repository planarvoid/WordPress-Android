package com.soundcloud.android.events;

import java.util.HashMap;
import java.util.Map;

public class PlayerLifeCycleEvent extends Event {

    public static final int STATE_IDLE = 0;
    public static final int STATE_DESTROYED = 1;

    public static PlayerLifeCycleEvent forIdle() {
        return new PlayerLifeCycleEvent(STATE_IDLE, new HashMap<String, String>());
    }

    public static PlayerLifeCycleEvent forDestroyed() {
        return new PlayerLifeCycleEvent(STATE_DESTROYED, new HashMap<String, String>());
    }

    private PlayerLifeCycleEvent(int state, Map<String, String> attributes) {
        super(state, attributes);
    }

}
