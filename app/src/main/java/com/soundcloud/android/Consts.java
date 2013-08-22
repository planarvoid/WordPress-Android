package com.soundcloud.android;

import com.soundcloud.android.model.act.Activities;

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

    // dot file to have it excluded from media scanning - also use .nomedia
    public static final File EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH,  ".lrucache");
    public static final File OLD_EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH,  ".cache");
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

        int SIGNUP_VIA_FACEBOOK                 = 8001;
        int RECOVER_CODE                        = 8002;
        int SIGNUP_VIA_GOOGLE                   = 8003;
        int RECOVER_FROM_PLAY_SERVICES_ERROR    = 8004;
    }

    public static interface Keys {
        String WAS_SIGNUP = "wasSignup";
        String ONBOARDING = "onboarding";
    }

    public static interface StringValues {
        String ERROR = "error";
    }

    public static interface SdkSwitches {
        boolean useCustomNotificationLayouts = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        boolean useRichNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        boolean canDetermineActivityBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
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
        // TODO: still used in location picker
        int REFRESH = 202;
    }

    public interface GeneralIntents {
        String ACTIVITIES_UNSEEN_CHANGED = Activities.class.getSimpleName() + ".unseen_changed";
        String UNAUTHORIZED = SoundCloudApplication.class.getSimpleName() + ".unauthorized";
    }

    // these need to be unique across app
    public interface Notifications {
        int RECORD_NOTIFY_ID               = 0;
        int PLAYBACK_NOTIFY_ID             = 1;
        int UPLOADING_NOTIFY_ID            = 2;
        int DASHBOARD_NOTIFY_STREAM_ID     = 4;
        int DASHBOARD_NOTIFY_ACTIVITIES_ID = 5;
    }

    public interface ResourceStaleTimes {
        long user       =   86400000l;    //24*60*60*1000 = 24hr
        long track      =   14400000l;    //4*60*60*1000  = 4hr
        long playlist   =   14400000l;    //4*60*60*1000  = 4hr
        long activity   =   1200000l;     //20*60*1000    = 20 mins
    }

    public interface PrefKeys {
        String SC_PLAYQUEUE_URI                     = "sc_playlist_uri";
        String STREAMING_WRITES_SINCE_CLEANUP       = "streamingWritesSinceCleanup";
        String C2DM_DEVICE_URL                      = "c2dm.device_url";
        String C2DM_REG_TO_DELETE                   = "c2dm.to_delete";
        String NOTIFICATIONS_FOLLOWERS              = "notificationsFollowers";
        String NOTIFICATIONS_WIFI_ONLY              = "notificationsWifiOnly";
        String NOTIFICATIONS_INCOMING               = "notificationsIncoming";
        String NOTIFICATIONS_LIKES                  = "notificationsFavoritings";
        String NOTIFICATIONS_REPOSTS                = "notificationsReposts";
        String NOTIFICATIONS_COMMENTS               = "notificationsComments";
        String NOTIFICATIONS_FREQUENCY              = "notificationsFrequency";
        String VERSION_KEY                          = "changeLogVersionCode";
        String PLAYBACK_ERROR_REPORTING_ENABLED     = "playbackErrorReportingEnabled";
        String LAST_USER_SYNC                       = "lastUserSync";

        String DEV_HTTP_PROXY                       = "dev.http.proxy";
        String DEV_ALARM_CLOCK_ENABLED              = "dev.alarmClock.enabled";
        String DEV_ALARM_CLOCK_URI                  = "dev.alarmClock.uri";
    }
}
