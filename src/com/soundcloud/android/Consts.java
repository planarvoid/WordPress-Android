package com.soundcloud.android;

import android.os.Environment;

import java.io.File;
import java.security.Key;
import java.util.EnumSet;

public final class Consts {
    public static final File DB_PATH = new File("/data/data/com.soundcloud.android/databases/");

    @Deprecated
    public static final File DEPRECATED_EXTERNAL_STORAGE_DIRECTORY =
            new File(Environment.getExternalStorageDirectory(), "Soundcloud");

    // this directory will be preserved across reinstalls - e.g. used for recordings
    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "SoundCloud");

    // general purpose storage, removed on reinstall
    public static final File FILES_PATH = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files");

    // dot file to have it excluded from media scanning - also use .nomedia
    public static final File EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH, ".cache");
    public static final File EXTERNAL_STREAM_DIRECTORY = new File(FILES_PATH, "stream");

    @Deprecated
    public static final File EXTERNAL_TRACK_CACHE_DIRECTORY = new File(FILES_PATH, ".s");

    public static final long MAX_IMAGE_CACHE = 5 * 1024  * 1024; // 5  MB


    public interface IntentActions {
        public static final String CONNECTION_ERROR = "com.soundcloud.android.connectionerror";
        public static final String COMMENT_ADDED = "com.soundcloud.android.commentadded";
    }

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED = 2;
        int DIALOG_CANCEL_UPLOAD = 3;
        int DIALOG_RESET_RECORDING = 5;
        int DIALOG_UNSAVED_RECORDING = 6;
        int DIALOG_DELETE_RECORDING = 7;
        int DIALOG_ADD_COMMENT = 8;
        int DIALOG_LOGOUT = 9;
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

    public interface TourActivityIndexes {
        int RECORD = 0;
        int SHARE = 1;
        int FOLLOW = 2;
        int COMMENT = 3;
        int YOU = 4;
    }

    public enum GraphicSize {
        T500("t500x500", 500, 500),
        CROP("crop", 400, 400),
        T300("t300x300", 300, 400),
        LARGE("large", 100, 100),
        T67("t67x67", 67, 67),
        BADGE("badge", 47, 47),
        SMALL("small", 100, 100),
        TINY_ARTWORK("tiny", 20, 20),
        TINY_AVATAR("tiny", 18, 18),
        MINI("mini", 16, 16),
        Unknown("large", 100, 100);

        public final int width;
        public final int height;
        public final String key;


        GraphicSize(String key, int width, int height) {
            this.key = key;
            this.width = width;
            this.height = height;
        }
        static GraphicSize fromString(String s) {
            for (GraphicSize gs : EnumSet.allOf(GraphicSize.class)) {
                if (gs.key.equalsIgnoreCase(s)) return gs;
            }
            return Unknown;
        }
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

    public interface Tracking {
        String LABEL_DOMAIN_PREFIX = "http://soundcloud.com/";
        interface Categories {
            String AUTH      = "auth";
            String CONNECT   = "connect";
            String TRACKS    = "tracks";
            String RECORDING = "recording";
            String AUDIO_MESSAGE = "audio_message";
            String SHARE     = "share";
            String ERROR     = "error";
            String PLAYBACK_ERROR = "playbackError";
            String TOUR      = "tour";
        }

        interface Actions {
            String TRACK_PLAY = "Track Play";
            String TEN_PERCENT = "10percent";
            String NINTY_FIVE_PERCENT = "95percent";
            String TRACK_COMPLETE = "Track Complete";
        }

        String STREAM   = "/incoming";
        String ACTIVITY = "/activity";
        String RECORD   = "/record";
        String PLAYER   = "/player";
        @Deprecated String RECORD_RECORDING = "/record/recording";
        @Deprecated String RECORD_COMPLETE  = "/record/complete";
        @Deprecated String SHARE_PUBLIC     = "/record/share/public";
        @Deprecated String SHARE_PRIVATE    = "/record/share/private";
        @Deprecated String AUDIO_MESSAGE_RECORDING = "/audio_message/recording";
        @Deprecated String AUDIO_MESSAGE_COMPLETE  = "/audio_message/complete";
        String SEARCH        = "/search";
        String SEARCH_TRACKS = "/search?type=users&q=";
        String SEARCH_USERS  = "/search?type=tracks&q=";
        @Deprecated String LOGIN    = "/login";
        @Deprecated String LOGGED_OUT = "/loggedout";
        @Deprecated String SIGNUP   = "/signup";
        String SIGNUP_DETAILS       = "/signup/details";
        String PEOPLE_FINDER        = "/people/finder";
        String SETTINGS             = "/settings";
        String TRACKS_BY_TAG        = "/tracks_by_tag/";
        String TRACKS_BY_GENRE      = "/tracks_by_genre/";
    }
}
