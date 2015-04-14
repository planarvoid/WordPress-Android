package com.soundcloud.android.creators.record;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.creators.upload.Encoder;
import com.soundcloud.android.creators.upload.ImageResizer;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.creators.upload.Processor;
import com.soundcloud.android.creators.upload.UploadMonitorFragment;
import com.soundcloud.android.creators.upload.Uploader;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                RecordFragment.class,
                MetadataFragment.class,
                UploadMonitorFragment.class
        })
public class RecordModule {
}
