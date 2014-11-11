package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.stream.SoundStreamModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {ApiSyncService.class, SyncAdapterService.class}, includes = SoundStreamModule.class)
public class SyncModule {

}
