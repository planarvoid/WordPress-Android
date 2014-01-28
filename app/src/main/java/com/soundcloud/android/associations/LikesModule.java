package com.soundcloud.android.associations;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false,
        injects = {LikesListFragment.class},
        includes = {StorageModule.class, ApiModule.class})
public class LikesModule {
}
