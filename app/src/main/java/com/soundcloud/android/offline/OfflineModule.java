package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                OfflineContentService.class
        })
public class OfflineModule {
}
