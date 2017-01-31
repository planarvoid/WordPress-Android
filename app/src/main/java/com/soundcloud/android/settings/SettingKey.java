package com.soundcloud.android.settings;

public class SettingKey {

    /*
     * These keys correspond to values in keys_settings.xml
     */

    // GeneralSettings
    public static final String AUTOPLAY_RELATED_ENABLED = "autoplay_related_enabled";
    public static final String ANALYTICS_ENABLED = "analytics_enabled";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String VERSION = "version";
    public static final String CRASH_REPORTING_ENABLED = "acra.enable";

    // DeveloperSettings
    public static final String DEV_RECORDING_TYPE = "dev.defaultRecordingType";
    public static final String DEV_RECORDING_TYPE_RAW = "raw";
    public static final String DEV_FLUSH_EVENTLOGGER_INSTANTLY = "dev.flushEventloggerInstantly";

    // Offline
    static final String OFFLINE_COLLECTION = "offline.offlineCollections";
    static final String WIFI_ONLY = "offline.wifiOnlySync";
    static final String OFFLINE_STORAGE_LIMIT = "offline.storageLimit";
    static final String OFFLINE_REMOVE_ALL_OFFLINE_CONTENT = "offline.removeAllOfflineContent";

    // Legal
    static final String TERMS_OF_SERVICE = "terms_of_service";
    static final String PRIVACY_POLICY = "privacy_policy";
    static final String IMPRINT = "imprint";
    static final String GO_TERMS = "go_terms";

}
