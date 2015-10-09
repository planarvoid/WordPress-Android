package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.PropellerRx;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(complete = false, library = true)
public class StorageModule {

    public static final String PLAYLIST_TAGS = "PlaylistTags";
    public static final String DEVICE_MANAGEMENT = "DeviceManagement";
    public static final String PAYMENTS = "Payments";
    public static final String DEVICE_KEYS = "DeviceKeys";
    public static final String OFFLINE_SETTINGS = "OfflineSettings";
    public static final String FEATURES = "Features";
    public static final String STREAM_SYNC = "StreamSync";
    public static final String POLICY_SETTINGS = "Policies";
    public static final String RECOMMENDATIONS_SYNC = "RecommendationsSync";
    public static final String GCM = "gcm";
    public static final String FACEBOOK_INVITES = "FacebookInvites";
    public static final String COLLECTIONS = "collections";
    public static final String STATIONS = "stations";

    private static final String PREFS_PLAYLIST_TAGS = "playlist_tags";
    private static final String PREFS_DEVICE_MANAGEMENT = "device_management";
    private static final String PREFS_PAYMENTS = "payments";
    private static final String PREFS_DEVICE_KEYS = "device_keys";
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String PREFS_FEATURES = "features_settings";
    private static final String PREFS_POLICY_SETTINGS = "policy_settings";
    private static final String PREFS_STREAM_SYNC = "StreamSync";
    private static final String PREFS_RECOMMENDATIONS_SYNC = "RecommendationsSync";
    private static final String PREFS_GCM = "gcm";
    private static final String PREFS_FACEBOOK_INVITES = "facebook_invites";
    private static final String PREFS_COLLECTIONS = "collections";
    private static final String PREFS_STATIONS = "stations";

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

    @Provides @Singleton
    @Named(FEATURES)
    public SharedPreferences provideFeaturePrefs(Context context, Obfuscator obfuscator) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURES, Context.MODE_PRIVATE), obfuscator);
    }

    @Provides
    @Named(STREAM_SYNC)
    public SharedPreferences provideStreamSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_STREAM_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(RECOMMENDATIONS_SYNC)
    public SharedPreferences provideRecommendationsSyncPrefs(Context context) {
        return context.getSharedPreferences(PREFS_RECOMMENDATIONS_SYNC, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(FACEBOOK_INVITES)
    public SharedPreferences provideFacebookInvitesPrefs(Context context) {
        return context.getSharedPreferences(PREFS_FACEBOOK_INVITES, Context.MODE_PRIVATE);
    }

    @Provides
    @Named(STATIONS)
    public SharedPreferences provideSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_STATIONS, Context.MODE_PRIVATE);
    }

    @Provides
    public SQLiteDatabase provideDatabase(Context context) {
        return DatabaseManager.getInstance(context).getWritableDatabase();
    }

    @Provides
    public PropellerDatabase providePropeller(SQLiteDatabase database) {
        final PropellerDatabase propeller = new PropellerDatabase(database);
        propeller.setAssertBackgroundThread();
        return propeller;
    }

    @Provides
    public PropellerRx providePropellerRxWrapper(PropellerDatabase propeller) {
        return new PropellerRx(propeller);
    }
}
