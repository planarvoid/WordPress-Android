package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = ApiSyncService.class)
public class SyncModule {
}
