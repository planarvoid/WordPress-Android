package com.soundcloud.android.sync;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import com.soundcloud.android.sync.stream.SoundStreamSyncModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {ApiSyncService.class, SyncAdapterService.class},
        includes = {
                LikesSyncModule.class,
                EntitySyncModule.class,
                PostsSyncModule.class,
                SoundStreamSyncModule.class
        })
public class SyncModule {

}
