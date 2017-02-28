package com.soundcloud.android;

public final class Actions {
    // public intent API (do not change)
    public static final String RECORD = BuildConfig.APPLICATION_ID + ".action.RECORD";
    public static final String RECORD_START = BuildConfig.APPLICATION_ID + ".action.RECORD_START";
    public static final String RECORD_STOP = BuildConfig.APPLICATION_ID + ".action.RECORD_STOP";
    public static final String STREAM = BuildConfig.APPLICATION_ID + ".action.STREAM";
    public static final String ACTIVITY = BuildConfig.APPLICATION_ID + ".action.ACTIVITY";
    public static final String DISCOVERY = BuildConfig.APPLICATION_ID + ".action.DISCOVERY";
    public static final String SEARCH = BuildConfig.APPLICATION_ID + ".action.SEARCH";
    public static final String PERFORM_SEARCH = BuildConfig.APPLICATION_ID + ".action.PERFORM_SEARCH";
    public static final String SHARE = BuildConfig.APPLICATION_ID + ".SHARE";
    public static final String EDIT = BuildConfig.APPLICATION_ID + ".EDIT";
    public static final String USER_BROWSER = BuildConfig.APPLICATION_ID + ".action.USER_BROWSER";
    public static final String PLAYLIST = BuildConfig.APPLICATION_ID + ".action.PLAYLIST";
    public static final String TRACK = BuildConfig.APPLICATION_ID + ".action.TRACK";
    public static final String COLLECTION = BuildConfig.APPLICATION_ID + ".action.COLLECTION";
    public static final String MORE = BuildConfig.APPLICATION_ID + ".action.MORE";

    // recording share from third-party apps
    public static final String EXTRA_TITLE = BuildConfig.APPLICATION_ID + ".extra.title";
    public static final String EXTRA_DESCRIPTION = BuildConfig.APPLICATION_ID + ".extra.description";
    public static final String EXTRA_PUBLIC = BuildConfig.APPLICATION_ID + ".extra.public";
    public static final String EXTRA_TAGS = BuildConfig.APPLICATION_ID + ".extra.tags";
    public static final String EXTRA_GENRE = BuildConfig.APPLICATION_ID + ".extra.genre";
    public static final String EXTRA_ARTWORK = BuildConfig.APPLICATION_ID + ".extra.artwork";

    // internal actions
    public static final String ACCOUNT_ADDED = BuildConfig.APPLICATION_ID + ".action.ACCOUNT_ADDED";

    // launcher shortcut actions (some duplications from above for easier tracking)
    public static final String SHORTCUT_PLAY_LIKES = BuildConfig.APPLICATION_ID + ".action.SHORTCUT_PLAY_LIKES";
    public static final String SHORTCUT_SEARCH = BuildConfig.APPLICATION_ID + ".action.SHORTCUT_SEARCH";

    public static final String UPLOAD = BuildConfig.APPLICATION_ID + ".actions.upload";
    public static final String UPLOAD_MONITOR = BuildConfig.APPLICATION_ID + ".actions.upload.monitor";
}
