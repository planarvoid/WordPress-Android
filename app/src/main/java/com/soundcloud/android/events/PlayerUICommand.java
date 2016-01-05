package com.soundcloud.android.events;

public final class PlayerUICommand {

    private static final int EXPAND_PLAYER = 0;
    private static final int COLLAPSE_PLAYER = 1;
    private static final int LOCK_PLAYER_EXPANDED = 2;
    private static final int UNLOCK_PLAYER = 3;

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

    /**
     * Signal any on-screen instance of the player to be locked into the expanded state.
     */
    public static PlayerUICommand lockPlayerExpanded() {
        return new PlayerUICommand(LOCK_PLAYER_EXPANDED);
    }

    /**
     * Signal any on-screen instance of the player to be unlocked.
     */
    public static PlayerUICommand unlockPlayer() {
        return new PlayerUICommand(UNLOCK_PLAYER);
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

    public boolean isLockExpanded() {
        return kind == LOCK_PLAYER_EXPANDED;
    }

    public boolean isUnlock() {
        return kind == UNLOCK_PLAYER;
    }

    @Override
    public String toString() {
        return "player UI command: " + kind;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PlayerUICommand command = (PlayerUICommand) o;
        return this.kind == command.kind;
    }
}
