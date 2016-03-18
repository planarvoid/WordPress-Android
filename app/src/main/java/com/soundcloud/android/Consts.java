package com.soundcloud.android;

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
    public static final File OLD_EXTERNAL_CACHE_DIRECTORY = new File(FILES_PATH, ".cache");
    public static final File EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY = new File(FILES_PATH, "stream");

    public static final int LIST_PAGE_SIZE = 30;
    public static final int CARD_PAGE_SIZE = 20;

    public static final class RequestCodes {
        public static final int GALLERY_IMAGE_PICK = 9000;
        public static final int GALLERY_IMAGE_TAKE = 9001;

        public static final int RECOVER_PASSWORD_CODE = 8002;
        public static final int SIGNUP_VIA_GOOGLE = 8003;
        public static final int RECOVER_FROM_PLAY_SERVICES_ERROR = 8004;
    }

    public static final class GeneralIntents {
        public static final String UNAUTHORIZED = SoundCloudApplication.class.getSimpleName() + ".unauthorized";
    }

    public static final class ResourceStaleTimes {
        public static final long USER = TimeUnit.DAYS.toMillis(1);
        public static final long TRACK = TimeUnit.HOURS.toMillis(4);
        public static final long PLAYLIST = TimeUnit.HOURS.toMillis(4);
        public static final long ACTIVITY = TimeUnit.MINUTES.toMillis(20);
    }

    public static final class PrefKeys {
        public static final String C2DM_DEVICE_URL = "c2dm.device_url";
        public static final String LAST_USER_SYNC = "lastUserSync";
        public static final String DEV_HTTP_PROXY = "dev.http.proxy";
    }
}
