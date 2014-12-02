package com.soundcloud.android.creators.record;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.creators.upload.UploadMonitorActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                UploadActivity.class,
                UploadMonitorActivity.class
        })
public class RecordModule {
}
