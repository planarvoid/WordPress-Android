package com.soundcloud.android.events;

public final class PlayerUICommand {

    private static final int EXPAND_PLAYER = 0;
    private static final int COLLAPSE_PLAYER = 1;

    private final int kind;

    /**
     * Signals any on-screen instance of the player to expand. It will be made visible if hidden.
     */
    public static PlayerUICommand expandPlayer() {
        return new PlayerUICommand(EXPAND_PLAYER);
    }

    /**
     * Signal any on-screen instance of the player to collapse. It will be made visible if hidden.
     */
    public static PlayerUICommand collapsePlayer() {
        return new PlayerUICommand(COLLAPSE_PLAYER);
    }

    private PlayerUICommand(int kind) {
        this.kind = kind;
    }

    public boolean isExpand() {
        return kind == EXPAND_PLAYER;
    }

    public boolean isCollapse() {
        return kind == COLLAPSE_PLAYER;
    }

    @Override
    public String toString() {
        return "player UI command: " + kind;
    }

}
