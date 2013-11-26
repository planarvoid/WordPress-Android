package com.soundcloud.android;

import com.soundcloud.android.model.ScModelManager;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

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
    public Context provideContext(){
        return mApplication;
    }

    @Provides
    public ScModelManager provideModelManager() {
        return SoundCloudApplication.MODEL_MANAGER;
    }

    @Provides
    public Resources provideResources(){
        return mApplication.getResources();
    }

    @Provides
    public AccountManager provideAccountManager(){
        return AccountManager.get(mApplication);
    }


    @Provides
    public ContentResolver provideContentResolver(){
        return mApplication.getContentResolver();
    }

    @Provides
    public SharedPreferences provideDefaultSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(mApplication);
    }

}
