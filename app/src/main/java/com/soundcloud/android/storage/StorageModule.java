package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
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

    public static final String PLAYLIST_TAGS = "playlist_tags";

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }

    @Provides
    @Named("PlaylistTags")
    public SharedPreferences provideSharedPreferences(SoundCloudApplication application) {
        return application.getSharedPreferences(PLAYLIST_TAGS, Context.MODE_PRIVATE);
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
    public Scheduler provideDatabaseScheduler() {
        return ScSchedulers.STORAGE_SCHEDULER;
    }
}
