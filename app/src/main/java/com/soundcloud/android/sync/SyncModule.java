package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.stream.SoundStreamModule;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {ApiSyncService.class, SyncAdapterService.class},
        includes = {SoundStreamModule.class, LikesSyncModule.class, EntitySyncModule.class})
public class SyncModule {

}
