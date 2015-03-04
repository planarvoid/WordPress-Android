package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.stream.SoundStreamModule;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {ApiSyncService.class, SyncAdapterService.class},
        includes = {
                SoundStreamModule.class,
                LikesSyncModule.class,
                EntitySyncModule.class,
                PostsSyncModule.class
        })
public class SyncModule {

}
