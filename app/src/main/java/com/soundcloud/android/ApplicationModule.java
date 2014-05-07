package com.soundcloud.android;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(library = true, includes = {ApiModule.class, StorageModule.class}, injects = SoundCloudApplication.class)
public class ApplicationModule {

    private final SoundCloudApplication application;

    public ApplicationModule(SoundCloudApplication application) {
        this.application = application;
    }

    @Provides
    public SoundCloudApplication provideApplication() {
        return application;
    }

    @Provides
    public Context provideContext() {
        return application;
    }

    @Provides
    public Resources provideResources() {
        return application.getResources();
    }

    @Provides
    public AccountManager provideAccountManager() {
        return AccountManager.get(application);
    }

    @Provides
    public SharedPreferences provideDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    public ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    public TelephonyManager provideTelephonyManager() {
        return (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Provides
    public LayoutInflater provideLayoutInflater() {
        return LayoutInflater.from(application);
    }

    @Provides
    @Singleton
    public ScModelManager provideModelManager() {
        return new ScModelManager(application);
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        return new EventBus();
    }

    @Provides
    @Named("MainLooper")
    public Looper providesMainLooper() {
        return Looper.getMainLooper();
    }

    @Provides
    public SoundRecorder provideSoundRecorder() {
        return SoundRecorder.getInstance(application);
    }
}
