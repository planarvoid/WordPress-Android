package com.soundcloud.android.events;

public class PlayerUIEvent {

    public static final int PLAYER_EXPANDED = 0;
    public static final int PLAYER_COLLAPSED = 1;
    public static final int PLAY_TRIGGERED = 2;

    private final int kind;

    public static PlayerUIEvent fromPlayerExpanded() {
        return new PlayerUIEvent(PLAYER_EXPANDED);
    }

    public static PlayerUIEvent fromPlayerCollapsed() {
        return new PlayerUIEvent(PLAYER_COLLAPSED);
    }

    /**
     * Fires whenever the user clicks on a track cell, which should cause any on screen instances of the player to expand
     * Note: We can't decide on a better name for this. Feel free :)
     */
    public static PlayerUIEvent forPlayTriggered() {
        return new PlayerUIEvent(PLAY_TRIGGERED);
    }

    public PlayerUIEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }
}
