package com.soundcloud.android.settings;

public class SettingKey {

    /*
     *These keys correspond to values in settings_keys.xml
     */

    // GeneralSettings
    public static final String WIRELESS = "wireless";
    public static final String AUTOPLAY_RELATED_ENABLED = "autoplay_related_enabled";
    public static final String LOGOUT = "logout";
    public static final String HELP = "help";
    public static final String ANALYTICS_ENABLED = "analytics_enabled";
    public static final String CLEAR_CACHE = "clearCache";
    public static final String GENERAL_SETTINGS = "generalSettings";
    public static final String OFFLINE_SYNC_SETTINGS = "offlineSyncSettings";
    public static final String NOTIFICATION_SETTINGS = "notificationSettings";
    public static final String LEGAL = "legal";
    public static final String VERSION = "version";
    public static final String CRASH_REPORTING_ENABLED = "acra.enable";

    // DeveloperSettings
    public static final String DEV_CLEAR_NOTIFICATIONS = "dev.clearNotifications";
    public static final String DEV_REWIND_NOTIFICATIONS = "dev.rewindNotifications";
    public static final String DEV_SYNC_NOW = "dev.syncNow";
    public static final String DEV_CRASH = "dev.crash";
    public static final String DEV_HTTP_PROXY = "dev.http.proxy";
    public static final String DEV_RECORDING_TYPE = "dev.defaultRecordingType";
    public static final String DEV_RECORDING_TYPE_RAW = "raw";
    public static final String DEV_FLUSH_EVENTLOGGER_INSTANTLY = "dev.flushEventloggerInstantly";

    // Offline
    public static final String OFFLINE_COLLECTION = "offline.offlineCollections";
    public static final String WIFI_ONLY = "offline.wifiOnlySync";
    public static final String OFFLINE_STORAGE_LIMIT = "offline.storageLimit";
    public static final String OFFLINE_REMOVE_ALL_OFFLINE_CONTENT = "offline.removeAllOfflineContent";
    public static final String BUY_SUBSCRIPTION = "buySubscription";
    public static final String RESTORE_SUBSCRIPTION = "restoreSubscription";

    // Legal
    public static final String TERMS_OF_SERVICE = "terms_of_service";
    public static final String PRIVACY_POLICY = "privacy_policy";
    public static final String IMPRINT = "imprint";
    public static final String GO_TERMS = "go_terms";

}
