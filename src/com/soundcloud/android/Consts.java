package com.soundcloud.android;

import android.os.Environment;

import java.io.File;

public class Consts {
    public static final String DEPRECATED_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/Overcast";
    public static final String NEW_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/SoundCloud.db";

    public static final File DEPRECATED_EXTERNAL_STORAGE_DIRECTORY =
            new File(Environment.getExternalStorageDirectory(), "Soundcloud");

    public static final File EXTERNAL_CACHE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files/.cache/");

    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "SoundCloud");

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED = 2;
        int DIALOG_CANCEL_UPLOAD = 3;
        int DIALOG_RESET_RECORDING = 5;
        int DIALOG_UNSAVED_RECORDING = 6;
    }

    public interface OptionsMenu {
        int SETTINGS = 200;
        int VIEW_CURRENT_TRACK = 201;
        int REFRESH = 202;
        int CANCEL_CURRENT_UPLOAD = 203;
        int INCOMING = 204;
        int FRIEND_FINDER = 205;
    }

    public interface GraphicsSizes {
        String T500 = "t500x500";
        String CROP = "crop";
        String LARGE = "large";
        String BADGE = "badge";
        String SMALL = "small";
    }

    public interface ListId {
        int LIST_INCOMING = 1001;
        int LIST_EXCLUSIVE = 1002;
        int LIST_USER_TRACKS = 1003;
        int LIST_USER_FAVORITES = 1004;
        int LIST_USER_FOLLOWINGS = 1006;
        int LIST_USER_FOLLOWERS = 1007;
        int LIST_USER_SUGGESTED = 1008;
    }

    // these need to be unique across app
    public interface Notifications {
        int RECORD_NOTIFY_ID = 0;
        int PLAYBACK_NOTIFY_ID = 1;
        int UPLOAD_NOTIFY_ID = 2;
    }
}
