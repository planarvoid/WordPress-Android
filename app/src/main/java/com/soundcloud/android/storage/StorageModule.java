package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;

@Module(complete = false, library = true)
public class StorageModule {

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }
}
