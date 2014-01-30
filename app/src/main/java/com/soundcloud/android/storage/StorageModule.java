package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import dagger.Module;
import dagger.Provides;

import android.content.ContentResolver;

@Module(library = true, complete = false)
public class StorageModule {

    @Provides
    public ContentResolver provideContentResolver(SoundCloudApplication application) {
        return application.getContentResolver();
    }
}
