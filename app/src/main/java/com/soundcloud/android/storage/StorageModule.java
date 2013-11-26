package com.soundcloud.android.storage;

import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.content.ContentResolver;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(library = true, complete = false)
public class StorageModule {

    @Provides
    @Named("StorageScheduler")
    public Scheduler provideStorageScheduler(){
        return ScSchedulers.STORAGE_SCHEDULER;
    }

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
