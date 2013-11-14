package com.soundcloud.android;

import com.soundcloud.android.model.ScModelManager;
import dagger.Module;
import dagger.Provides;

@Module(library = true)
public class ApplicationModule {

    private final SoundCloudApplication mApplication;

    public ApplicationModule(SoundCloudApplication application) {
        this.mApplication = application;
    }

    @Provides
    public SoundCloudApplication provideApplication() {
        return mApplication;
    }

    @Provides
    public ScModelManager provideModelManager() {
        return SoundCloudApplication.MODEL_MANAGER;
    }

}
