package com.soundcloud.android.creators.record;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.creators.upload.UploadMonitorFragment;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                RecordActivity.class,
                RecordFragment.class,
                MetadataFragment.class,
                UploadMonitorFragment.class,
                UploadActivity.class
        })
public class RecordModule {
}
