package com.soundcloud.android.events;

public class PlayerUIEvent {

    public static final int PLAYER_EXPANDED = 0;
    public static final int PLAYER_COLLAPSED = 1;
    public static final int EXPAND_PLAYER = 2;
    public static final int COLLAPSE_PLAYER = 3;
    public static final int SHOW_PLAYER = 4;

    private final int kind;

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

    public boolean isExpand() {
        return kind == PLAYER_EXPANDED || kind == EXPAND_PLAYER;
    }

    @Override
    public String toString() {
        return "player UI event: " + kind;
    }

}
