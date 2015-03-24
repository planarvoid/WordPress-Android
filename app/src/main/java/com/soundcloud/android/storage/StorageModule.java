package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.PropellerRx;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Named;

@Module(complete = false, library = true)
public class StorageModule {

    private static final String PLAYLIST_TAGS = "playlist_tags";
    private static final String DEVICE_MANAGEMENT = "device_management";
    private static final String PAYMENTS = "payments";
    private static final String DEVICE_KEYS = "device_keys";
    private static final String OFFLINE_SETTINGS = "offline_settings";
    private static final String FEATURE_SETTINGS = "features_settings";

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }

    @Provides
    @Named("PlaylistTags")
    public SharedPreferences providePlaylistTagPrefs(Context context) {
        return context.getSharedPreferences(PLAYLIST_TAGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named("DeviceManagement")
    public SharedPreferences provideDeviceManagementPrefs(Context context) {
        return context.getSharedPreferences(DEVICE_MANAGEMENT, Context.MODE_PRIVATE);
    }

    @Provides
    @Named("Payments")
    public SharedPreferences providePaymentsPrefs(Context context) {
        return context.getSharedPreferences(PAYMENTS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named("DeviceKeys")
    public SharedPreferences provideKeysPrefs(Context context) {
        return context.getSharedPreferences(DEVICE_KEYS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named("OfflineSettings")
    public SharedPreferences provideOfflinePrefs(Context context) {
        return context.getSharedPreferences(OFFLINE_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    @Named("Features")
    public SharedPreferences provideFeaturePrefs(Context context) {
        return context.getSharedPreferences(FEATURE_SETTINGS, Context.MODE_PRIVATE);
    }

    @Provides
    public SQLiteDatabase provideDatabase(Context context) {
        return DatabaseManager.getInstance(context).getWritableDatabase();
    }

    @Provides
    public DatabaseScheduler provideDatabaseScheduler(PropellerDatabase database) {
        return new DatabaseScheduler(database, ScSchedulers.STORAGE_SCHEDULER);
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

    @Provides
    @Named("Storage")
    public Scheduler provideStorageScheduler() {
        return ScSchedulers.STORAGE_SCHEDULER;
    }
}
