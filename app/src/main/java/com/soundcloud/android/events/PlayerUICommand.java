package com.soundcloud.android.events;

public class PlayerUICommand {

    private static final int EXPAND_PLAYER = 0;
    private static final int COLLAPSE_PLAYER = 1;
    private static final int SHOW_PLAYER = 2;

    private final int kind;

    /**
     * Signals any on-screen instance of the player to expand.
     */
    public static PlayerUICommand expandPlayer() {
        return new PlayerUICommand(EXPAND_PLAYER);
    }

    /**
     * Signal any on-screen instance of the player to collapse.
     */
    public static PlayerUICommand collapsePlayer() {
        return new PlayerUICommand(COLLAPSE_PLAYER);
    }

    /**
     * Signals player panel to show panel (become visible).
     */
    public static PlayerUICommand showPlayer() {
        return new PlayerUICommand(SHOW_PLAYER);
    }

    public PlayerUICommand(int kind) {
        this.kind = kind;
    }

    public boolean isExpand() {
        return kind == EXPAND_PLAYER;
    }

    public boolean isCollapse() {
        return kind == COLLAPSE_PLAYER;
    }

    public boolean isShow() {
        return kind == SHOW_PLAYER;
    }

    @Override
    public String toString() {
        return "player UI command: " + kind;
    }

}
