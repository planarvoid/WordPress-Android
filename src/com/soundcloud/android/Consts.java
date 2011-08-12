package com.soundcloud.android;

import android.os.Environment;

import java.io.File;

public final class Consts {
    public static final File DB_PATH = new File("/data/data/com.soundcloud.android/databases/");
    public static final File DEPRECATED_DB_ABS_PATH = new File(DB_PATH, "Overcast");
    public static final File NEW_DB_ABS_PATH = new File(DB_PATH, "SoundCloud.db");

    public static final File DEPRECATED_EXTERNAL_STORAGE_DIRECTORY =
            new File(Environment.getExternalStorageDirectory(), "Soundcloud");

    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "SoundCloud");

    public static final File FILES_PATH = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files");

    public static final File EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH, ".cache");
    public static final File EXTERNAL_TRACK_CACHE_DIRECTORY = new File(FILES_PATH, ".s");

    public static final long TRACK_MAX_CACHE = 200 * 1024 * 1024; // 200 MB
    public static final long TRACK_MIN_CACHE = 20 * 1024  * 1024; // 20  MB

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED = 2;
        int DIALOG_CANCEL_UPLOAD = 3;
        int DIALOG_RESET_RECORDING = 5;
        int DIALOG_UNSAVED_RECORDING = 6;
        int DIALOG_LOGOUT = 7;
    }

    public interface OptionsMenu {
        int SETTINGS = 200;
        int VIEW_CURRENT_TRACK = 201;
        int REFRESH = 202;
        int CANCEL_CURRENT_UPLOAD = 203;
        int STREAM = 204;
        int FRIEND_FINDER = 205;
        int UPLOAD_FILE = 206;
        int FILTER = 207;
    }

    public interface GraphicsSizes {
        String T500 = "t500x500";
        String CROP = "crop";
        String LARGE = "large";
        String BADGE = "badge";
        String SMALL = "small";
    }

    public interface ListId {
        int LIST_STREAM          = 1001;
        int LIST_ACTIVITY        = 1002;
        int LIST_USER_TRACKS     = 1003;
        int LIST_USER_FAVORITES  = 1004;
        int LIST_USER_FOLLOWINGS = 1006;
        int LIST_USER_FOLLOWERS  = 1007;
        int LIST_USER_SUGGESTED  = 1008;
    }

    // these need to be unique across app
    public interface Notifications {
        int RECORD_NOTIFY_ID    = 0;
        int PLAYBACK_NOTIFY_ID  = 1;
        int UPLOAD_NOTIFY_ID    = 2;
        int DASHBOARD_NOTIFY_STREAM_ID = 3;
        int DASHBOARD_NOTIFY_ACTIVITIES_ID = 4;
        int BETA_NOTIFY_ID    = 5;
    }

    public interface TrackingEvents {
        String STREAM = "/incoming";
        String ACTIVITY = "/activity";
        String RECORD = "/record";
        String RECORD_RECORDING = "/record/recording";
        String RECORD_COMPLETE = "/record/complete";
        String SHARE_PUBLIC = "/record/share/public";
        String SHARE_PRIVATE = "/record/share/private";
        String SEARCH = "/search";
        String SEARCH_TRACKS = "/search/tracks/q=";
        String SEARCH_USERS = "/search/users/q=";
        String LOGGED_OUT = "/loggedout";
        String LOGIN = "/login";
        String SIGNUP = "/signup";
        String SIGNUP_DETAILS = "/signup/details";
        String PEOPLE_FINDER = "/people/finder";
        String SETTINGS = "/settings";
        String TRACKS_BY_TAG = "/tracks_by_tag/";
        String TRACKS_BY_GENRE = "/tracks_by_genre/";

        String AUDIO_ENGINE = "/internal/audioEngine";
    }
}
