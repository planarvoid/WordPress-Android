package com.soundcloud.android;

public final class Consts {
    public static final int NOT_SET = -1;

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

    public static final class PrefKeys {
        public static final String C2DM_DEVICE_URL = "c2dm.device_url";
        public static final String LAST_USER_SYNC = "lastUserSync";
        public static final String DEV_HTTP_PROXY = "dev.http.proxy";
    }
}
