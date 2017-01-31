package com.soundcloud.android.events;

public final class PlayerUICommand {

    private static final int SHOW_PLAYER = 0;
    private static final int EXPAND_PLAYER = 1;
    private static final int AUTOMATIC_COLLAPSE_PLAYER = 2;
    private static final int LOCK_PLAYER_EXPANDED = 3;
    private static final int UNLOCK_PLAYER = 4;
    private static final int FORCE_PLAYER_LANDSCAPE = 5;
    private static final int FORCE_PLAYER_PORTRAIT = 6;
    private static final int HIDE_PLAYER = 7;
    private static final int LOCK_FOR_PLAY_QUEUE = 8;
    private static final int UNLOCK_FOR_PLAY_QUEUE = 9;
    private static final int MANUAL_COLLAPSE_PLAYER = 10;

    private final int kind;

    /**
     * Signals any on-screen instance of the player to be shown. If player was hidden, we will show it in the collapsed state
     */
    public static PlayerUICommand showPlayer() {
        return new PlayerUICommand(SHOW_PLAYER);
    }

    /**
     * Signals any on-screen instance of the player to be hidden.
     */
    public static PlayerUICommand hidePlayer() {
        return new PlayerUICommand(HIDE_PLAYER);
    }

    /**
     * Signals any on-screen instance of the player to expand. It will be made visible if hidden.
     */
    public static PlayerUICommand expandPlayer() {
        return new PlayerUICommand(EXPAND_PLAYER);
    }

    /**
     * Signal any on-screen instance of the player to collapse. It will be made visible if hidden.
     */
    public static PlayerUICommand collapsePlayerAutomatically() {
        return new PlayerUICommand(AUTOMATIC_COLLAPSE_PLAYER);
    }

    public static PlayerUICommand collapsePlayerManually() {
        return new PlayerUICommand(MANUAL_COLLAPSE_PLAYER);
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

    /**
     * Signal any on-screen instance of the player to be forced into the landscape orientation.
     */
    public static PlayerUICommand forcePlayerLandscape() {
        return new PlayerUICommand(FORCE_PLAYER_LANDSCAPE);
    }

    /**
     * Signal any on-screen instance of the player to be forced into the portrait orientation.
     */
    public static PlayerUICommand forcePlayerPortrait() {
        return new PlayerUICommand(FORCE_PLAYER_PORTRAIT);
    }


    public static PlayerUICommand lockPlayQueue() {
        return new PlayerUICommand(LOCK_FOR_PLAY_QUEUE);
    }

    public static PlayerUICommand unlockPlayQueue() {
        return new PlayerUICommand(UNLOCK_FOR_PLAY_QUEUE);
    }


    private PlayerUICommand(int kind) {
        this.kind = kind;
    }

    public boolean isShow() {
        return kind == SHOW_PLAYER;
    }

    public boolean isHide() {
        return kind == HIDE_PLAYER;
    }

    public boolean isExpand() {
        return kind == EXPAND_PLAYER;
    }

    public boolean isAutomaticCollapse() {
        return kind == AUTOMATIC_COLLAPSE_PLAYER;
    }

    public boolean isManualCollapse() {
        return kind == MANUAL_COLLAPSE_PLAYER;
    }

    public boolean isLockExpanded() {
        return kind == LOCK_PLAYER_EXPANDED;
    }

    public boolean isUnlock() {
        return kind == UNLOCK_PLAYER;
    }

    public boolean isForceLandscape() {
        return kind == FORCE_PLAYER_LANDSCAPE;
    }

    public boolean isForcePortrait() {
        return kind == FORCE_PLAYER_PORTRAIT;
    }

    public boolean isLockPlayQueue() {
        return kind == LOCK_FOR_PLAY_QUEUE;
    }

    public boolean isUnlockPlayQueue() {
        return kind == UNLOCK_FOR_PLAY_QUEUE;
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
