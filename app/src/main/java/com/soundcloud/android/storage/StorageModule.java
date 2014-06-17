package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import dagger.Module;
import dagger.Provides;

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
}
