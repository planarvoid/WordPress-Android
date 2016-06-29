package com.soundcloud.android.events;

import rx.functions.Func1;

public class PlayerUIEvent {

    public static final int PLAYER_EXPANDED = 0;
    public static final int PLAYER_COLLAPSED = 1;
    public static final int PLAYQUEUE_DISPLAYED = 2;
    public static final int PLAYQUEUE_HIDDEN = 3;

    private final int kind;

    public static final Func1<PlayerUIEvent, Boolean> PLAYER_IS_COLLAPSED = new Func1<PlayerUIEvent, Boolean>() {
        @Override
        public Boolean call(PlayerUIEvent playerUIEvent) {
            return playerUIEvent.getKind() == PlayerUIEvent.PLAYER_COLLAPSED;
        }
    };

    /**
     * Panel is completely expanded.
     */
    public static PlayerUIEvent fromPlayerExpanded() {
        return new PlayerUIEvent(PLAYER_EXPANDED);
    }

    /**
     * Panel is completely collapsed.
     */
    public static PlayerUIEvent fromPlayerCollapsed() {
        return new PlayerUIEvent(PLAYER_COLLAPSED);
    }

    public static PlayerUIEvent fromPlayQueueDisplayed() {
        return new PlayerUIEvent(PLAYQUEUE_DISPLAYED);
    }

    public static PlayerUIEvent fromPlayQueueHidden() {
        return new PlayerUIEvent(PLAYQUEUE_HIDDEN);
    }

    public PlayerUIEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "player UI event: " + kind;
    }
}
