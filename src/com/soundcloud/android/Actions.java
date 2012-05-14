package com.soundcloud.android;

import android.content.Intent;

public final class Actions {
    // public intent API (do not change)
    public static final String RECORD       = "com.soundcloud.android.action.RECORD";
    public static final String MESSAGE      = "com.soundcloud.android.action.MESSAGE";
    public static final String STREAM       = "com.soundcloud.android.action.STREAM";
    public static final String ACTIVITY     = "com.soundcloud.android.action.ACTIVITY";
    public static final String SEARCH       = "com.soundcloud.android.action.SEARCH";
    public static final String PROFILE      = "com.soundcloud.android.action.PROFILE";
    public static final String PLAYER       = "com.soundcloud.android.action.PLAYER";
    public static final String MY_PROFILE   = "com.soundcloud.android.action.MY_PROFILE";
    public static final String SHARE        = "com.soundcloud.android.SHARE";
    public static final String EDIT         = "com.soundcloud.android.EDIT";
    public static final String ACCOUNT_PREF = "com.soundcloud.android.action.ACCOUNT_PREF";
    public static final String USER_BROWSER = "com.soundcloud.android.action.USER_BROWSER";
    public static final String RECORDING_PROCESS = "com.soundcloud.android.recording.PROCESS";

    public static final String EXTRA_TITLE       = "com.soundcloud.android.extra.title";
    public static final String EXTRA_WHERE       = "com.soundcloud.android.extra.where";
    public static final String EXTRA_DESCRIPTION = "com.soundcloud.android.extra.description";
    public static final String EXTRA_PUBLIC      = "com.soundcloud.android.extra.public";
    public static final String EXTRA_LOCATION    = "com.soundcloud.android.extra.location" ;
    public static final String EXTRA_TAGS        = "com.soundcloud.android.extra.tags" ;
    public static final String EXTRA_GENRE       = "com.soundcloud.android.extra.genre" ;
    public static final String EXTRA_ARTWORK     = "com.soundcloud.android.extra.artwork";

    // internal actions
    public static final String ACCOUNT_ADDED       = "com.soundcloud.android.action.ACCOUNT_ADDED";
    public static final String CHANGE_PROXY_ACTION = "com.soundcloud.android.action.CHANGE_PROXY";
    public static final String EXTRA_PROXY         = "proxy"; // proxy URL as string
    public static final String CONNECTION_ERROR    = "com.soundcloud.android.connectionerror";
    public static final String LOGGING_OUT         = "com.soundcloud.android.loggingout";
    public static final String COMMENT_ADDED       = "com.soundcloud.android.commentadded";
    public static final String RESEND              = "com.soundcloud.android.RESEND";
    public static final String ALARM               = "com.soundcloud.android.actions.ALARM";
    public static final String CANCEL_ALARM        = "com.soundcloud.android.actions.CANCEL_ALARM";

    public static final String UPLOAD              = "com.soundcloud.android.actions.upload";
    public static final String UPLOAD_CANCEL       = "com.soundcloud.android.actions.upload.cancel";
    public static final String UPLOAD_MONITOR      = "com.soundcloud.android.actions.upload.monitor";
}
