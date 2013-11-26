package com.soundcloud.android.storage;

import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;

import javax.inject.Singleton;

@Module(library = true, complete = false)
public class StorageModule {

    @Provides
    @Singleton
    public UserDAO provideUserDAO(ContentResolver contentResolver){
        return new UserDAO(contentResolver);
    }

    @Provides
    @Singleton
    public TrackDAO provideTrackDAO(ContentResolver contentResolver){
        return new TrackDAO(contentResolver);
    }

}
