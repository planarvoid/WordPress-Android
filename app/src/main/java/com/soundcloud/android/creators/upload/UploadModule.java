package com.soundcloud.android.creators.upload;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                Uploader.class,
                Processor.class,
                ImageResizer.class,
                Encoder.class
        })
public class UploadModule {
}
