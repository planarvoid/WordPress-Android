package com.soundcloud.android.events;

public class PlayerUIEvent {

    public static final int PLAYER_EXPANDING = 0;
    public static final int PLAYER_COLLAPSING = 1;
    public static final int EXPAND_PLAYER = 2;
    public static final int COLLAPSE_PLAYER = 3;
    public static final int SHOW_PLAYER = 4;
    public static final int PLAYER_COLLAPSED = 5;

    private final int kind;

    /**
     * Panel is expanding and player UI should configure to full-screen mode.
     */
    public static PlayerUIEvent fromPlayerExpanding() {
        return new PlayerUIEvent(PLAYER_EXPANDING);
    }

    /**
     * Panel is collapsing and player UI should configure to footer mode.
     */
    public static PlayerUIEvent fromPlayerCollapsing() {
        return new PlayerUIEvent(PLAYER_COLLAPSING);
    }

    /**
     * Panel has finished collapsing.
     */
    public static PlayerUIEvent fromPlayerCollapsed() {
        return new PlayerUIEvent(PLAYER_COLLAPSED);
    }

    /**
     * Signals any on-screen instance of the player to expand.
     */
    public static PlayerUIEvent forExpandPlayer() {
        return new PlayerUIEvent(EXPAND_PLAYER);
    }

    /**
     * Signal any on-screen instance of the player to collapse.
     */
    public static PlayerUIEvent forCollapsePlayer() {
        return new PlayerUIEvent(COLLAPSE_PLAYER);
    }

    /**
     * Signals player panel to show panel (become visible).
     */
    public static PlayerUIEvent forShowPlayer() {
        return new PlayerUIEvent(SHOW_PLAYER);
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
