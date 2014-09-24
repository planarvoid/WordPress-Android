package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.BigPlaybackNotificationPresenter;
import com.soundcloud.android.playback.service.PlaybackNotificationPresenter;
import com.soundcloud.android.playback.service.RichNotificationPresenter;
import com.soundcloud.android.playback.service.managers.FroyoRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.ICSRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.eventbus.DefaultEventBus;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CrashlyticsMemoryReporter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.MemoryReporter;
import com.soundcloud.android.waveform.WaveformData;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.telephony.TelephonyManager;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

@Module(library = true, includes = {ApiModule.class, StorageModule.class})
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
    public NotificationManager provideNotificationManager() {
        return (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    public ActivityManager provideActivityManager() {
        return (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Provides
    public LocalBroadcastManager provideLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(application);
    }

    @Provides
    @Singleton
    public ScModelManager provideModelManager() {
        return new ScModelManager(application);
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        return new DefaultEventBus();
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


    @Provides
    public AppWidgetManager provideAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    @Provides
    public NotificationCompat.Builder provideNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context);
    }

    @Provides
    @Singleton
    public PlaybackNotificationPresenter providePlaybackNotificationPresenter(Context context, ApplicationProperties applicationProperties,
                                                                              NotificationPlaybackRemoteViews.Factory factory,
                                                                              Provider<NotificationCompat.Builder> builder) {
        if (applicationProperties.shouldUseBigNotifications()) {
            return new BigPlaybackNotificationPresenter(context, factory, builder);
        } else if (applicationProperties.shouldUseRichNotifications()) {
            return new RichNotificationPresenter(context, factory, builder);
        } else {
            return new PlaybackNotificationPresenter(context, builder);
        }
    }

    @SuppressWarnings("unchecked")
    @Provides
    @Singleton
    public IRemoteAudioManager provideRemoteAudioManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                return new ICSRemoteAudioManager(context);
            } catch (Exception e) {
                ErrorUtils.handleSilentException("Could not create remote audio manager", e);
            }
        }
        return new FroyoRemoteAudioManager(context);
    }

    @Singleton
    @Provides
    public LruCache<Urn, WaveformData> provideWaveformCache() {
        return new LruCache<>(DEFAULT_WAVEFORM_CACHE_SIZE);
    }

    @Singleton
    @Provides
    public MemoryReporter provideMemoryReporter(ActivityManager activityManager) {
        if (application.isReportingCrashes()) {
            return new CrashlyticsMemoryReporter(activityManager);
        } else {
            return new MemoryReporter(activityManager);
        }
    }

    @SuppressWarnings("unchecked")
    @Provides
    public ImageProcessor provideImageProcessor(Context context) {
        return new ImageProcessorCompat();
    }
}
