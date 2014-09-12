package com.soundcloud.android;

import com.soundcloud.android.api.legacy.model.activities.Activities;

import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class Consts {
    public static final int NOT_SET = -1;

    // this directory will be preserved across re-installs - e.g. used for recordings
    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(), "SoundCloud");

    // general purpose storage, removed on reinstall
    public static final File FILES_PATH = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files");

    // dot file to have it excluded from media scanning - also use .nomedia
    public static final File EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH, ".lrucache");
    public static final File OLD_EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH, ".cache");
    public static final File EXTERNAL_STREAM_DIRECTORY = new File(FILES_PATH, "stream");

    @Deprecated
    public static final File EXTERNAL_TRACK_CACHE_DIRECTORY = new File(FILES_PATH, ".s");

    @Deprecated
    public static final File DEPRECATED_EXTERNAL_STORAGE_DIRECTORY =
            new File(Environment.getExternalStorageDirectory(), "Soundcloud");

    public static final int LIST_PAGE_SIZE = 30;
    public static final int CARD_PAGE_SIZE = 20;
    public static final int MAX_COMMENTS_TO_LOAD = 50;


    public static final class RequestCodes {
        public static final int GALLERY_IMAGE_PICK = 9000;
        public static final int GALLERY_IMAGE_TAKE = 9001;
        public static final int PICK_VENUE = 9003;
        public static final int MAKE_CONNECTION = 9004;

        public static final int SIGNUP_VIA_FACEBOOK = 8001;
        public static final int RECOVER_CODE = 8002;
        public static final int SIGNUP_VIA_GOOGLE = 8003;
        public static final int RECOVER_FROM_PLAY_SERVICES_ERROR = 8004;
    }

    public static final class SdkSwitches {
        public static final boolean USE_CUSTOM_NOTIFICATION_LAYOUTS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        public static final boolean USE_RICH_NOTIFICATIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        public static final boolean CAN_DETERMINE_ACTIVITY_BACKGROUND = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        public static final boolean USE_CUSTOM_FONTS = Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1 ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    }

    public static final class Dialogs {
        public static final int DIALOG_ERROR_LOADING = 1;
        public static final int DIALOG_LOGOUT = 9;
        public static final int DIALOG_TRANSCODING_FAILED = 12;
        public static final int DIALOG_TRANSCODING_PROCESSING = 13;
    }

    public static final class GeneralIntents {
        public static final String ACTIVITIES_UNSEEN_CHANGED = Activities.class.getSimpleName() + ".unseen_changed";
        public static final String UNAUTHORIZED = SoundCloudApplication.class.getSimpleName() + ".unauthorized";
    }

    // these need to be unique across app
    public static final class Notifications {
        public static final int RECORD_NOTIFY_ID = 0;
        public static final int PLAYBACK_NOTIFY_ID = 1;
        public static final int UPLOADING_NOTIFY_ID = 2;
        public static final int DASHBOARD_NOTIFY_STREAM_ID = 4;
        public static final int DASHBOARD_NOTIFY_ACTIVITIES_ID = 5;
    }

    public static final class ResourceStaleTimes {
        public static final long USER = TimeUnit.DAYS.toMillis(1);
        public static final long TRACK = TimeUnit.HOURS.toMillis(4);
        public static final long PLAYLIST = TimeUnit.HOURS.toMillis(4);
        public static final long ACTIVITY = TimeUnit.MINUTES.toMillis(20);
    }

    public static final class PrefKeys {

        public static final String STREAMING_WRITES_SINCE_CLEANUP = "streamingWritesSinceCleanup";
        public static final String C2DM_DEVICE_URL = "c2dm.device_url";
        public static final String C2DM_REG_TO_DELETE = "c2dm.to_delete";
        public static final String NOTIFICATIONS_FOLLOWERS = "notificationsFollowers";
        public static final String NOTIFICATIONS_WIFI_ONLY = "notificationsWifiOnly";
        public static final String NOTIFICATIONS_INCOMING = "notificationsIncoming";
        public static final String NOTIFICATIONS_LIKES = "notificationsFavoritings";
        public static final String NOTIFICATIONS_REPOSTS = "notificationsReposts";
        public static final String NOTIFICATIONS_COMMENTS = "notificationsComments";
        public static final String NOTIFICATIONS_FREQUENCY = "notificationsFrequency";
        public static final String PLAYBACK_ERROR_REPORTING_ENABLED = "playbackErrorReportingEnabled";
        public static final String LAST_USER_SYNC = "lastUserSync";
        public static final String LAST_EMAIL_CONFIRMATION_REMINDER = "confirmation_last_reminded";

        public static final String DEV_HTTP_PROXY = "dev.http.proxy";
    }
}
