package com.soundcloud.android;

import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

public final class Consts {
    // this directory will be preserved across re-installs - e.g. used for recordings
    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "SoundCloud");

    // general purpose storage, removed on reinstall
    public static final File FILES_PATH = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files");

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

    public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
    public static final String AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";


    public static interface RequestCodes {
        int GALLERY_IMAGE_PICK  = 9000;
        int GALLERY_IMAGE_TAKE  = 9001;
        int PICK_EMAILS         = 9002;
        int PICK_VENUE          = 9003;
        int MAKE_CONNECTION     = 9004;
        int IMAGE_CROP          = 9005;
    }

    public static interface SdkSwitches {
        boolean useCustomNotificationLayouts = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        boolean useRichNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        boolean canDetermineActivityBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    public interface SecretCodes {
        String TOGGLE_ERROR_REPORTING = "12345";
    }

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED = 2;
        int DIALOG_DISCARD_RECORDING = 5;
        int DIALOG_UNSAVED_RECORDING = 6;
        int DIALOG_DELETE_RECORDING = 7;
        int DIALOG_ADD_COMMENT = 8;
        int DIALOG_LOGOUT = 9;
        int DIALOG_INSTALL_PROCESSOR = 10;
        int DIALOG_REVERT_RECORDING = 11;
        int DIALOG_TRANSCODING_FAILED = 12;
        int DIALOG_TRANSCODING_PROCESSING = 13;
    }

    public interface OptionsMenu {
        int SETTINGS = 200;
        int VIEW_CURRENT_TRACK = 201;
        int REFRESH = 202;
        int STREAM = 204;
        int FRIEND_FINDER = 205;
        int FILTER = 206;
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

        public static GraphicSize getListItemGraphicSize(Context c) {
            if (ImageUtils.isScreenXL(c)) {
                return GraphicSize.LARGE;
            } else {
                if (c.getResources().getDisplayMetrics().density > 1) {
                    return GraphicSize.LARGE;
                } else {
                    return GraphicSize.BADGE;
                }
            }
        }

        public static String formatUriForSearchSuggestionsList(Context c, String uri) {
            return getSearchSuggestionsListItemGraphicSize(c).formatUri(uri);
        }

        public static GraphicSize getSearchSuggestionsListItemGraphicSize(Context c) {
            if (ImageUtils.isScreenXL(c)) {
                return GraphicSize.T67;
            } else {
                if (c.getResources().getDisplayMetrics().density > 1) {
                    return GraphicSize.BADGE;
                } else {
                    return GraphicSize.SMALL;
                }
            }
        }

        public static String formatUriForPlayer(Context c, String uri) {
            return getPlayerGraphicSize(c).formatUri(uri);
        }

        public static GraphicSize getPlayerGraphicSize(Context c) {
            // for now, just return T500. logic will come with more screen support
            return GraphicSize.T500;
        }

        public String formatUri(String uri) {
            if (TextUtils.isEmpty(uri)) return null;
            if (uri.contains(GraphicSize.LARGE.key) && GraphicSize.LARGE != this) {
                return uri.replace(GraphicSize.LARGE.key, key);
            } else if (uri.contains(GraphicSize.TINY_ARTWORK.key) &&
                    GraphicSize.TINY_ARTWORK != this && GraphicSize.TINY_AVATAR != this) {
                return uri.replace(GraphicSize.TINY_ARTWORK.key, key);
            } else {
                return uri;
            }
        }

        public static GraphicSize getMinimumSizeFor(int width, int height, boolean fillDimensions) {
            GraphicSize valid = null;
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

    // these need to be unique across app
    public interface Notifications {
        int RECORD_NOTIFY_ID    = 0;
        int PLAYBACK_NOTIFY_ID  = 1;

        int UPLOADING_NOTIFY_ID = 2;
        int UPLOADED_NOTIFY_ID  = 3;

        int DASHBOARD_NOTIFY_STREAM_ID = 4;
        int DASHBOARD_NOTIFY_ACTIVITIES_ID = 5;
    }

    public interface ResourceStaleTimes {
        long user = 86400000;       //24*60*60*1000 = 24hr
        long track = 3600000l;      //60*60*1000 = 1hr
        long activity = 600000l;    //30*60*1000 = 10 mins
    }

    public interface PrefKeys {
        String EXCLUSIVE_ONLY_KEY                   = "incoming_exclusive_only";
        String SC_PLAYQUEUE_URI = "sc_playlist_uri";
        String STREAMING_WRITES_SINCE_CLEANUP       = "streamingWritesSinceCleanup";
        String C2DM_DEVICE_URL                      = "c2dm.device_url";
        String C2DM_REG_TO_DELETE                   = "c2dm.to_delete";
        String LAST_SYNC_CLEANUP                    = "lastSyncCleanup";
        String NOTIFICATIONS_FOLLOWERS              = "notificationsFollowers";
        String NOTIFICATIONS_WIFI_ONLY              = "notificationsWifiOnly";
        String NOTIFICATIONS_INCOMING               = "notificationsIncoming";
        String NOTIFICATIONS_EXCLUSIVE              = "notificationsExclusive";
        String NOTIFICATIONS_FAVORITINGS            = "notificationsFavoritings";
        String NOTIFICATIONS_COMMENTS               = "notificationsComments";
        String NOTIFICATIONS_FREQUENCY              = "notificationsFrequency";
        String VERSION_KEY                          = "changeLogVersionCode";
        String PLAYBACK_ERROR_REPORTING_ENABLED     = "playbackErrorReportingEnabled";
        String LAST_USER_SYNC                       = "lastUserSync";
        String LAST_SHORTCUT_SYNC                   = "lastShortcutSync";

        String DEV_HTTP_PROXY                       = "dev.http.proxy";
        String DEV_ALARM_CLOCK_ENABLED              = "dev.alarmClock.enabled";
        String DEV_ALARM_CLOCK_URI                  = "dev.alarmClock.uri";
    }
}
