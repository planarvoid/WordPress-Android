package com.soundcloud.android;

import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public final class Consts {
    // this directory will be preserved across re-installs - e.g. used for recordings
    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "SoundCloud");

    // general purpose storage, removed on reinstall
    public static final File FILES_PATH = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files");

    public static final String VERSION_KEY = "changeLogVersionCode";

    // dot file to have it excluded from media scanning - also use .nomedia
    public static final File EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH,  ".cache");
    public static final File EXTERNAL_STREAM_DIRECTORY = new File(FILES_PATH, "stream");

    @Deprecated
    public static final File EXTERNAL_TRACK_CACHE_DIRECTORY = new File(FILES_PATH, ".s");

    @Deprecated
    public static final File DEPRECATED_EXTERNAL_STORAGE_DIRECTORY =
            new File(Environment.getExternalStorageDirectory(), "Soundcloud");

    public static final int COLLECTION_PAGE_SIZE      = 50;
    public static final int MAX_COMMENTS_TO_LOAD      = 50;

    // adapter loading constants
    public static final int ROW_APPEND_BUFFER = 6;
    public static final int ITEM_TYPE_LOADING = -1;

    public static interface SdkSwitches {
        boolean useRichNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        boolean canDetermineActivityBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED = 2;
        int DIALOG_RESET_RECORDING = 5;
        int DIALOG_UNSAVED_RECORDING = 6;
        int DIALOG_DELETE_RECORDING = 7;
        int DIALOG_ADD_COMMENT = 8;
        int DIALOG_LOGOUT = 9;
        int DIALOG_REVERT_RECORDING = 10;
    }

    public interface OptionsMenu {
        int SETTINGS = 200;
        int VIEW_CURRENT_TRACK = 201;
        int REFRESH = 202;
        int STREAM = 204;
        int FRIEND_FINDER = 205;
        int FILTER = 206;
        int SELECT_FILE = 207;
        int PRIVATE_MESSAGE = 208;
    }

    public enum GraphicSize {
        T500("t500x500", 500, 500),
        CROP("crop", 400, 400),
        T300("t300x300", 300, 400),
        LARGE("large", 100, 100),
        T67("t67x67", 67, 67),
        BADGE("badge", 47, 47),
        SMALL("small", 32, 32),
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

        public static GraphicSize fromString(String s) {
            for (GraphicSize gs : values()) {
                if (gs.key.equalsIgnoreCase(s)) return gs;
            }
            return Unknown;
        }

        public static String formatUriForList(Context c, String uri){
            return getListItemGraphicSize(c).formatUri(uri);
        }

        public static Consts.GraphicSize getListItemGraphicSize(Context c) {
            if (ImageUtils.isScreenXL(c)) {
                return Consts.GraphicSize.LARGE;
            } else {
                if (c.getResources().getDisplayMetrics().density > 1) {
                    return Consts.GraphicSize.LARGE;
                } else {
                    return Consts.GraphicSize.BADGE;
                }
            }
        }

        public static String formatUriForPlayer(Context c, String uri) {
            return getPlayerGraphicSize(c).formatUri(uri);
        }

        public static Consts.GraphicSize getPlayerGraphicSize(Context c) {
            // for now, just return T500. logic will come with more screen support
            return GraphicSize.T500;
        }

        public String formatUri(String uri) {
            if (uri == null) return null;
            else if (this == Consts.GraphicSize.LARGE) return uri;
            else return uri.replace(Consts.GraphicSize.LARGE.key, key);
        }

        public static Consts.GraphicSize getMinimumSizeFor(int width, int height, boolean fillDimensions) {
            Consts.GraphicSize valid = null;
            for (GraphicSize gs : values()) {
                if (fillDimensions){
                    if (gs.width >= width && gs.height >= height) {
                        valid = gs;
                    } else {
                        break;
                    }
                } else {
                    if (gs.width >= width || gs.height >= height) {
                        valid = gs;
                    } else {
                        break;
                    }
                }

            }
            return valid == null ? Unknown : valid;
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

        int UPLOADING_NOTIFY_ID = 2;
        int UPLOADED_NOTIFY_ID  = 3;

        int DASHBOARD_NOTIFY_STREAM_ID = 4;
        int DASHBOARD_NOTIFY_ACTIVITIES_ID = 5;
        int BETA_NOTIFY_ID    = 6;
    }

    public interface ResourceStaleTimes {
        long user = 86400000;       //24*60*60*1000 = 24hr
        long track = 3600000l;      //60*60*1000 = 1hr
        long activity = 600000l;    //30*60*1000 = 10 mins
    }
}
