package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.PropellerRx;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

@Module
public class StorageModule {

    public static final String STREAM_CACHE_DIRECTORY = "StreamCacheDirectory";
    public static final String PLAYLIST_TAGS = "PlaylistTags";
    public static final String DEVICE_MANAGEMENT = "DeviceManagement";
    public static final String PAYMENTS = "Payments";
    public static final String DEVICE_KEYS = "DeviceKeys";
    public static final String OFFLINE_SETTINGS = "OfflineSettings";
    public static final String FEATURES = "Features";
    public static final String STREAM_SYNC = "StreamSync";
    public static final String ACTIVITIES_SYNC = "ActivitiesSync";
    public static final String POLICY_SETTINGS = "Policies";
    public static final String CONFIGURATION_SETTINGS = "ConfigurationSettings";
    public static final String ANALYTICS_SETTINGS = "Analytics";
    public static final String RECOMMENDED_TRACKS_SYNC = "RecommendedTracksSync";
    public static final String CHARTS_SYNC = "ChartsSync";
    public static final String GCM = "gcm";
    public static final String FACEBOOK_INVITES = "FacebookInvites";
    public static final String COLLECTIONS = "collections";
    public static final String STATIONS = "stations";
    public static final String PLAYER = "player";
    public static final String SYNCER = "syncer";
    public static final String UPSELL = "upsell";
    public static final String NOTIFICATION_PREFERENCES = "NotificationPreferences";
    public static final String IMAGE_CONFIG = "ImageConfiguration";
    public static final String PLAY_SESSION_STATE = "PlaySessionState";
    public static final String FEATURES_FLAGS = "FeatureFlags";
    public static final String UNAUTHORIZED_ERRORS = "UnauthorizedErrors";

    public static final String PREFS_NOTIFICATION_PREFERENCES = "notification_preferences";
    public static final String PREFS_FEATURE_FLAGS = "feature_flags";

    private static final String PREFS_PLAYLIST_TAGS = "playlist_tags";
    private static final String PREFS_DEVICE_MANAGEMENT = "device_management";
    private static final String PREFS_PAYMENTS = "payments";
    private static final String PREFS_DEVICE_KEYS = "device_keys";
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String PREFS_FEATURES = "features_settings";
    private static final String PREFS_POLICY_SETTINGS = "policy_settings";
    private static final String PREFS_STREAM_SYNC = "StreamSync";
    private static final String PREFS_ACTIVITIES_SYNC = "ActivitiesSync";
    private static final String PREFS_RECOMMENDED_TRACKS_SYNC = "RecommendationsSync";
    private static final String PREFS_CHARTS_SYNC = "ChartsSync";
    private static final String PREFS_GCM = "gcm";
    private static final String PREFS_FACEBOOK_INVITES = "facebook_invites";
    private static final String PREFS_COLLECTIONS = "collections";
    private static final String PREFS_STATIONS = "stations";
    private static final String PREFS_PLAYER = "player";
    private static final String PREFS_SYNCER = "syncer";
    private static final String PREFS_UPSELL = "upsell";
    private static final String PREFS_ANALYTICS_SETTINGS = "analytics_settings";
    private static final String PREFS_CONFIGURATION_SETTINGS = "device_config_settings";
    private static final String PREFS_IMAGE_CONFIG = "image_configuration";
    private static final String PREFS_PLAY_SESSION_STATE = "play_session_state";
    private static final String PREFS_UNAUTHORIZED_ERRORS = "unauthorized_errors";

    @Provides
    @Named(STREAM_CACHE_DIRECTORY)
    @Nullable
    public File provideStreamCacheDirectory(Context context) {
        return IOUtils.getExternalStorageDir(context, "skippy");
    }

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }

    @Provides
    @Named(PLAYLIST_TAGS)
    public SharedPreferences providePlaylistTagPrefs(Context context) {
        return context.getSharedPreferences(PREFS_PLAYLIST_TAGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(GCM)
    public SharedPreferences provideGcmPrefs(Context context) {
        return context.getSharedPreferences(PREFS_GCM, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(COLLECTIONS)
    public SharedPreferences provideCollectionsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_COLLECTIONS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(DEVICE_MANAGEMENT)
    public SharedPreferences provideDeviceManagementPrefs(Context context) {
        return context.getSharedPreferences(PREFS_DEVICE_MANAGEMENT, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PAYMENTS)
    public SharedPreferences providePaymentsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_PAYMENTS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(DEVICE_KEYS)
    public SharedPreferences provideKeysPrefs(Context context) {
        return context.getSharedPreferences(PREFS_DEVICE_KEYS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(OFFLINE_SETTINGS)
    public SharedPreferences provideOfflinePrefs(Context context) {
        return context.getSharedPreferences(PREFS_OFFLINE_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(POLICY_SETTINGS)
    public SharedPreferences providePolicyPrefs(Context context) {
        return context.getSharedPreferences(PREFS_POLICY_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(CONFIGURATION_SETTINGS)
    public SharedPreferences provideConfigurationPrefs(Context context) {
        return context.getSharedPreferences(PREFS_CONFIGURATION_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(ANALYTICS_SETTINGS)
    public SharedPreferences provideAnalyticsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named(FEATURES)
    public SharedPreferences provideFeaturePrefs(Context context, Obfuscator obfuscator) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURES, Context.MODE_PRIVATE),
                                         obfuscator);
    }

    @Provides
    @Singleton
    @Named(FEATURES_FLAGS)
    public SharedPreferences provideFeatureFlagsPrefs(Context context, Obfuscator obfuscator) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURE_FLAGS, Context.MODE_PRIVATE),
                                         obfuscator);
    }

    @Provides
    @Named(STREAM_SYNC)
    public SharedPreferences provideStreamSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_STREAM_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(UPSELL)
    public SharedPreferences provideStreamPrefs(Context context) {
        return context.getSharedPreferences(PREFS_UPSELL, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(ACTIVITIES_SYNC)
    public SharedPreferences provideActivitiesSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_ACTIVITIES_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(RECOMMENDED_TRACKS_SYNC)
    public SharedPreferences provideRecommendedTracksSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_RECOMMENDED_TRACKS_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(CHARTS_SYNC)
    public SharedPreferences provideChartsSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_CHARTS_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(FACEBOOK_INVITES)
    public SharedPreferences provideFacebookInvitesPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FACEBOOK_INVITES, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(STATIONS)
    public SharedPreferences provideStationsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_STATIONS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PLAYER)
    public SharedPreferences providePlayerPreferences(Context context) {
        return context.getSharedPreferences(PREFS_PLAYER, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(SYNCER)
    public SharedPreferences provideSyncerPreferences(Context context) {
        return context.getSharedPreferences(PREFS_SYNCER, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(NOTIFICATION_PREFERENCES)
    public SharedPreferences provideNotificationPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(IMAGE_CONFIG)
    public SharedPreferences provideImageConfiguration(Context context) {
        return context.getSharedPreferences(PREFS_IMAGE_CONFIG, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PLAY_SESSION_STATE)
    public SharedPreferences providePlaySessionState(Context context) {
        return context.getSharedPreferences(PREFS_PLAY_SESSION_STATE, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named(FEATURES_FLAGS)
    public PersistentStorage provideFeatureFlagsStorage(@Named(FEATURES_FLAGS) SharedPreferences preferences) {
        return new PersistentStorage(preferences);
    }

    @Provides
    @Named(UNAUTHORIZED_ERRORS)
    public SharedPreferences provideUnauthorizedErrorsPreferences(Context context) {
        return getUnauthorizedErrorsSharedPreferences(context);
    }

    @Provides
    public SQLiteDatabase provideDatabase(Context context, ApplicationProperties applicationProperties) {
        return provideDatabaseManager(context, applicationProperties).getWritableDatabase();
    }

    @Provides
    public DatabaseManager provideDatabaseManager(Context context, ApplicationProperties applicationProperties) {
        return DatabaseManager.getInstance(context, applicationProperties);
    }

    @Provides
    public PropellerDatabase providePropeller(SQLiteDatabase database, ApplicationProperties applicationProperties) {
        final PropellerDatabase propeller;
        if (applicationProperties.shouldLogQueries()) {
            propeller = new PropellerDatabase(database, new DebugQueryHook());
        } else {
            propeller = new PropellerDatabase(database);
        }
        propeller.setAssertBackgroundThread();
        return propeller;
    }

    @Provides
    @Nullable
    public DebugQueryHook provideQueryHook(ApplicationProperties applicationProperties) {
        return applicationProperties.shouldLogQueries() ? new DebugQueryHook() : null;
    }

    @Provides
    public PropellerRx providePropellerRxWrapper(PropellerDatabase propeller) {
        return new PropellerRx(propeller);
    }

    // Exposing this, since a the dependent class (UnauthorisedRequestRegistry) is also used by legacy code
    // not using Dagger
    public static SharedPreferences getUnauthorizedErrorsSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_UNAUTHORIZED_ERRORS, Context.MODE_PRIVATE);
    }

}
