package com.soundcloud.android;

import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.model.ScModelManager;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import javax.inject.Singleton;

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

    @Provides
    public LayoutInflater provideLayoutInflater(){
        return LayoutInflater.from(mApplication);
    }

    @Provides
    @Singleton
    public ScModelManager provideModelManager() {
        return SoundCloudApplication.MODEL_MANAGER;
    }

    @Provides
    @Singleton
    public AnalyticsEngine provideAnalyticsEngine(Context context) {
        return AnalyticsEngine.getInstance(context);
    }

}
