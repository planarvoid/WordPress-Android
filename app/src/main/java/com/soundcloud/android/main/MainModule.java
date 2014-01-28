package com.soundcloud.android.main;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false,
        injects = {MainActivity.class},
        includes = {StorageModule.class, ApiModule.class})
public class MainModule {
}
