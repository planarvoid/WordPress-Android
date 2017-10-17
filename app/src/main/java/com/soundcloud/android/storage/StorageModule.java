package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivitiesCleanupHelper;
import com.soundcloud.android.collection.playhistory.PlayHistoryCleanupHelper;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedCleanupHelper;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.likes.LikeCleanupHelper;
import com.soundcloud.android.offline.OfflineContentCleanupHelper;
import com.soundcloud.android.playback.PlayQueueCleanupHelper;
import com.soundcloud.android.profile.PostsCleanupHelper;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.users.FollowingCleanupHelper;
import com.soundcloud.android.stations.StationsCleanupHelper;
import com.soundcloud.android.stream.StreamCleanupHelper;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsCleanupHelper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.android.waveform.WaveformCacheSerializer;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.PropellerRxV2;
import com.vincentbrison.openlibraries.android.dualcache.Builder;
import com.vincentbrison.openlibraries.android.dualcache.DualCache;
import dagger.Lazy;
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
import java.util.List;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class StorageModule {

    static final String DEBUG_PROPELLER_RX = "DebugPropellerRx";
    static final String DB_CLEANUP_HELPERS = "DatabaseCleanupHelpers";

    public static final String STREAM_CACHE_DIRECTORY_SKIPPY = "StreamCacheDirectorySkippy";
    public static final String STREAM_CACHE_DIRECTORY_FLIPPER = "StreamCacheDirectoryFlipper";
    public static final String WAVEFORM_CACHE = "WaveformCache";
    public static final String DEVICE_MANAGEMENT = "DeviceManagement";
    public static final String PAYMENTS = "Payments";
    public static final String DEVICE_KEYS = "DeviceKeys";
    public static final String OFFLINE_SETTINGS = "OfflineSettings";
    public static final String OFFLINE_CONTENT = "OfflineContent";
    public static final String FEATURES = "Features";
    public static final String STREAM_SYNC = "StreamSync";
    public static final String ACTIVITIES_SYNC = "ActivitiesSync";
    public static final String POLICY_SETTINGS = "Policies";
    public static final String CONFIGURATION_SETTINGS = "ConfigurationSettings";
    public static final String ANALYTICS_SETTINGS = "Analytics";
    public static final String GCM = "gcm";
    public static final String FACEBOOK_INVITES = "FacebookInvites";
    public static final String INTRODUCTORY_OVERLAYS = "IntroductoryOverlays";
    public static final String COLLECTIONS = "collections";
    public static final String STATIONS = "stations";
    public static final String PLAYER = "player";
    public static final String SYNCER = "syncer";
    public static final String ENTITY_SYNC_STATE = "entity_sync_state";
    public static final String UPSELL = "upsell";
    public static final String NOTIFICATION_PREFERENCES = "NotificationPreferences";
    public static final String IMAGE_CONFIG = "ImageConfiguration";
    public static final String PLAY_SESSION_STATE = "PlaySessionState";
    public static final String NAVIGATION_STATE = "NavigationState";
    public static final String FEATURES_FLAGS = "FeatureFlags";
    public static final String PREFS_NOTIFICATION_PREFERENCES = "notification_preferences";
    public static final String PREFS_FEATURE_FLAGS = "feature_flags";
    public static final String ADS = "ads";

    private static final String RECOMMENDED_TRACKS_SYNC = "RecommendedTracksSync";
    private static final String CHARTS_SYNC = "ChartsSync";
    private static final String UNAUTHORIZED_ERRORS = "UnauthorizedErrors";
    private static final String PREFS_DEVICE_MANAGEMENT = "device_management";
    private static final String PREFS_PAYMENTS = "payments";
    private static final String PREFS_DEVICE_KEYS = "device_keys";
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String PREFS_OFFLINE_CONTENT = "offline_content";
    private static final String PREFS_FEATURES = "features_settings";
    private static final String PREFS_POLICY_SETTINGS = "policy_settings";
    private static final String PREFS_STREAM_SYNC = "StreamSync";
    private static final String PREFS_ACTIVITIES_SYNC = "ActivitiesSync";
    private static final String PREFS_RECOMMENDED_TRACKS_SYNC = "RecommendationsSync";
    private static final String PREFS_CHARTS_SYNC = "ChartsSync";
    private static final String PREFS_GCM = "gcm";
    private static final String PREFS_FACEBOOK_INVITES = "facebook_invites";
    private static final String PREFS_INTRODUCTORY_OVERLAYS = "intro_overlays";
    private static final String PREFS_COLLECTIONS = "collections";
    private static final String PREFS_STATIONS = "stations";
    private static final String PREFS_PLAYER = "player";
    private static final String PREFS_SYNCER = "syncer";
    private static final String PREFS_ENTITY_SYNC_STATE = "entity_sync_state";
    private static final String PREFS_UPSELL = "upsell";
    private static final String PREFS_ANALYTICS_SETTINGS = "analytics_settings";
    private static final String PREFS_CONFIGURATION_SETTINGS = "device_config_settings";
    private static final String PREFS_IMAGE_CONFIG = "image_configuration";
    private static final String PREFS_PLAY_SESSION_STATE = "play_session_state";
    private static final String PREFS_UNAUTHORIZED_ERRORS = "unauthorized_errors";
    private static final String PREFS_ADS = "ads";
    private static final String PREFS_NAVIGATION_STATE = "navigation_state";

    @Provides
    @Named(STREAM_CACHE_DIRECTORY_SKIPPY)
    @Nullable
    static File provideStreamCacheDirectorySkippy(Context context) {
        return IOUtils.createExternalStorageDir(context, "skippy");
    }

    @Provides
    @Named(STREAM_CACHE_DIRECTORY_FLIPPER)
    @Nullable
    static File provideStreamCacheDirectoryFlipper(Context context) {
        return IOUtils.createExternalStorageDir(context, "flipper");
    }

    @Provides
    @Named(WAVEFORM_CACHE)
    @Singleton
    static DualCache<WaveformData> provideWaveformCache(Context context, WaveformCacheSerializer waveformCacheSerializer) {
        int maxSizeDiskMB = 1024 * 1024 * 2;
        int maxSizeRamMB = 1024 * 512;
        int version = 1;

        Builder<WaveformData> builder = new Builder<WaveformData>("waveform", version)
                .useSerializerInRam(maxSizeRamMB, waveformCacheSerializer)
                .enableLog();

        File storageDir = IOUtils.createExternalStorageDir(context, "waveform");
        if (storageDir != null) {
            builder.useSerializerInDisk(maxSizeDiskMB, storageDir, waveformCacheSerializer);
        } else {
            builder.noDisk();
        }

        return builder.build();
    }

    @Provides
    static ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }

    @Provides
    @Named(GCM)
    static SharedPreferences provideGcmPrefs(Context context) {
        return context.getSharedPreferences(PREFS_GCM, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(COLLECTIONS)
    static SharedPreferences provideCollectionsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_COLLECTIONS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(DEVICE_MANAGEMENT)
    static SharedPreferences provideDeviceManagementPrefs(Context context) {
        return context.getSharedPreferences(PREFS_DEVICE_MANAGEMENT, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PAYMENTS)
    static SharedPreferences providePaymentsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_PAYMENTS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(DEVICE_KEYS)
    static SharedPreferences provideKeysPrefs(Context context) {
        return context.getSharedPreferences(PREFS_DEVICE_KEYS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(OFFLINE_SETTINGS)
    static SharedPreferences provideOfflinePrefs(Context context) {
        return context.getSharedPreferences(PREFS_OFFLINE_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(OFFLINE_CONTENT)
    static SharedPreferences provideOfflineContentPrefs(Context context) {
        return context.getSharedPreferences(PREFS_OFFLINE_CONTENT, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(POLICY_SETTINGS)
    static SharedPreferences providePolicyPrefs(Context context) {
        return context.getSharedPreferences(PREFS_POLICY_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(CONFIGURATION_SETTINGS)
    static SharedPreferences provideConfigurationPrefs(Context context) {
        return context.getSharedPreferences(PREFS_CONFIGURATION_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(ANALYTICS_SETTINGS)
    static SharedPreferences provideAnalyticsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named(FEATURES)
    static SharedPreferences provideFeaturePrefs(Context context, Obfuscator obfuscator) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURES, Context.MODE_PRIVATE),
                                         obfuscator);
    }

    @Provides
    @Singleton
    @Named(FEATURES_FLAGS)
    static SharedPreferences provideFeatureFlagsPrefs(Context context, Obfuscator obfuscator) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURE_FLAGS, Context.MODE_PRIVATE),
                                         obfuscator);
    }

    @Provides
    @Named(ADS)
    static SharedPreferences provideAdsPrefs(Context context) {
        return context.getSharedPreferences(PREFS_ADS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(STREAM_SYNC)
    static SharedPreferences provideStreamSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_STREAM_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(UPSELL)
    static SharedPreferences provideStreamPrefs(Context context) {
        return context.getSharedPreferences(PREFS_UPSELL, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(ACTIVITIES_SYNC)
    static SharedPreferences provideActivitiesSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_ACTIVITIES_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(RECOMMENDED_TRACKS_SYNC)
    static SharedPreferences provideRecommendedTracksSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_RECOMMENDED_TRACKS_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(CHARTS_SYNC)
    static SharedPreferences provideChartsSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_CHARTS_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(FACEBOOK_INVITES)
    static SharedPreferences provideFacebookInvitesPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FACEBOOK_INVITES, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(STATIONS)
    static SharedPreferences provideStationsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_STATIONS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PLAYER)
    static SharedPreferences providePlayerPreferences(Context context) {
        return context.getSharedPreferences(PREFS_PLAYER, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(SYNCER)
    static SharedPreferences provideSyncerPreferences(Context context) {
        return context.getSharedPreferences(PREFS_SYNCER, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(ENTITY_SYNC_STATE)
    static SharedPreferences provideEntitySyncStatePreferences(Context context) {
        return context.getSharedPreferences(PREFS_ENTITY_SYNC_STATE, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(NOTIFICATION_PREFERENCES)
    static SharedPreferences provideNotificationPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(IMAGE_CONFIG)
    static SharedPreferences provideImageConfiguration(Context context) {
        return context.getSharedPreferences(PREFS_IMAGE_CONFIG, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(PLAY_SESSION_STATE)
    static SharedPreferences providePlaySessionState(Context context) {
        return context.getSharedPreferences(PREFS_PLAY_SESSION_STATE, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(INTRODUCTORY_OVERLAYS)
    static SharedPreferences provideIntroductoryOverlayPrefs(Context context) {
        return context.getSharedPreferences(PREFS_INTRODUCTORY_OVERLAYS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(NAVIGATION_STATE)
    static SharedPreferences provideNavigationStatePrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAVIGATION_STATE, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named(FEATURES_FLAGS)
    static PersistentStorage provideFeatureFlagsStorage(@Named(FEATURES_FLAGS) SharedPreferences preferences) {
        return new PersistentStorage(preferences);
    }

    @Provides
    @Named(UNAUTHORIZED_ERRORS)
    static SharedPreferences provideUnauthorizedErrorsPreferences(Context context) {
        return getUnauthorizedErrorsSharedPreferences(context);
    }

    @Provides
    static SQLiteDatabase provideDatabase(Context context, ApplicationProperties applicationProperties) {
        return provideDatabaseManager(context, applicationProperties).getWritableDatabase();
    }

    @Provides
    static DatabaseManager provideDatabaseManager(Context context, ApplicationProperties applicationProperties) {
        return DatabaseManager.getInstance(context, applicationProperties);
    }

    @Provides
    static PropellerDatabase providePropeller(SQLiteDatabase database, Lazy<DebugQueryHook> debugQueryHookLazy) {
        final PropellerDatabase propeller;
        propeller = new PropellerDatabase(database, debugQueryHookLazy.get());
        propeller.setAssertBackgroundThread();
        return propeller;
    }

    @Provides
    @Nullable
    static DebugQueryHook provideQueryHook(ApplicationProperties applicationProperties, Lazy<SlowQueryReporter> slowQueryReporterLazy) {
        return new DebugQueryHook(slowQueryReporterLazy.get(), applicationProperties.shouldLogQueries());
    }

    @Provides
    static PropellerRx providePropellerRxWrapper(PropellerDatabase propeller) {
        return new PropellerRx(propeller);
    }

    @Provides
    @Named(DEBUG_PROPELLER_RX)
    static PropellerRxV2 provideDebugPropellerRxWrapper(SQLiteDatabase database) {
        PropellerDatabase propeller = new PropellerDatabase(database);
        propeller.setAssertBackgroundThread();
        return new PropellerRxV2(propeller);
    }

    @Provides
    @Named(DB_CLEANUP_HELPERS)
    static List<CleanupHelper> provideCleanupHelpers(LikeCleanupHelper likeCleanupHelper,
                                              ActivitiesCleanupHelper activitiesCleanupHelper,
                                              PostsCleanupHelper postsCleanupHelper,
                                              FollowingCleanupHelper followingCleanupHelper,
                                              OfflineContentCleanupHelper offlineContentCleanupHelper,
                                              PlayQueueCleanupHelper playQueueCleanupHelper,
                                              StationsCleanupHelper stationsCleanupHelper,
                                              StreamCleanupHelper streamCleanupHelper,
                                              SuggestedCreatorsCleanupHelper suggestedCreatorsCleanupHelper,
                                              RecentlyPlayedCleanupHelper recentlyPlayedCleanupHelper,
                                              PlayHistoryCleanupHelper playHistoryCleanupHelper) {
        return Lists.newArrayList(likeCleanupHelper,
                                  activitiesCleanupHelper,
                                  postsCleanupHelper,
                                  followingCleanupHelper,
                                  offlineContentCleanupHelper,
                                  playQueueCleanupHelper,
                                  stationsCleanupHelper,
                                  streamCleanupHelper,
                                  suggestedCreatorsCleanupHelper,
                                  recentlyPlayedCleanupHelper,
                                  playHistoryCleanupHelper);
    }


    // Exposing this, since a the dependent class (UnauthorisedRequestRegistry) is also used by legacy code
    // not using Dagger
    public static SharedPreferences getUnauthorizedErrorsSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_UNAUTHORIZED_ERRORS, Context.MODE_PRIVATE);
    }

}
