package com.soundcloud.android;

import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

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
    public SharedPreferences provideDefaultSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(mApplication);
    }

    @Provides
    public LayoutInflater provideLayoutInflater(){
        return LayoutInflater.from(mApplication);
    }

    @Provides
    public ScModelManager provideModelManager() {
        return SoundCloudApplication.sModelManager;
    }

    @Provides
    public AnalyticsEngine provideAnalyticsEngine() {
        return mApplication.getAnalyticsEngine();
    }

    @Provides
    public ImageOperations provideImageOperations() {
        return ImageOperations.newInstance();
    }

    @Provides
    public PlayerAppWidgetProvider provideAppWidgetProvider() {
        return PlayerAppWidgetProvider.getInstance();
    }

    @Provides
    public EventBus2 provideEventBus() {
        return mApplication.getEventBus();
    }
}
