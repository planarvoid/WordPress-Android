package com.soundcloud.android.events;

public class PlayerUIEvent {

    public static final int PLAYER_EXPANDED = 0;
    public static final int PLAYER_COLLAPSED = 1;
    public static final int EXPAND_PLAYER = 2;
    public static final int COLLAPSE_PLAYER = 3;
    public static final int SHOW_PLAYER = 4;

    private final int kind;

    public static PlayerUIEvent fromPlayerExpanded() {
        return new PlayerUIEvent(PLAYER_EXPANDED);
    }

    public static PlayerUIEvent fromPlayerCollapsed() {
        return new PlayerUIEvent(PLAYER_COLLAPSED);
    }

    /**
     * Fired as a signal to expand any on-screen instance of the player.
     */
    public static PlayerUIEvent forExpandPlayer() {
        return new PlayerUIEvent(EXPAND_PLAYER);
    }

    public static PlayerUIEvent forCollapsePlayer() {
        return new PlayerUIEvent(COLLAPSE_PLAYER);
    }

    public static PlayerUIEvent forShowPlayer() {
        return new PlayerUIEvent(SHOW_PLAYER);
    }

    public PlayerUIEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

}
