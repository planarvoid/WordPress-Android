package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.content.ContentResolver;

import javax.inject.Named;

@Module(library = true, complete = false)
public class StorageModule {

    @Provides
    @Named("StorageScheduler")
    public Scheduler provideStorageScheduler(){
        return ScSchedulers.STORAGE_SCHEDULER;
    }

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }
}
