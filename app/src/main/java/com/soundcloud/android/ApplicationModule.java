package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.DefaultCastConnectionHelper;
import com.soundcloud.android.cast.UselessCastConnectionHelper;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.notification.BasicNotificationBuilder;
import com.soundcloud.android.playback.notification.BigNotificationBuilder;
import com.soundcloud.android.playback.notification.MediaStyleNotificationBuilder;
import com.soundcloud.android.playback.notification.NotificationBuilder;
import com.soundcloud.android.playback.notification.RichNotificationBuilder;
import com.soundcloud.android.playback.service.managers.FroyoRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.ICSRemoteAudioManager;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.eventbus.DefaultEventBus;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.android.view.menu.PopupMenuWrapperCompat;
import com.soundcloud.android.view.menu.PopupMenuWrapperICS;
import com.soundcloud.android.waveform.WaveformData;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
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
    public NotificationBuilder providesNotificationBuilderWrapper(Context context,
                                                                  ApplicationProperties applicationProperties,
                                                                  NotificationPlaybackRemoteViews.Factory remoteViewsFactory,
                                                                  FeatureFlags featureFlags) {
        if (featureFlags.isEnabled(Feature.ANDROID_L_MEDIA_NOTIFICATION)
                && applicationProperties.shouldUseMediaStyleNotifications()) {
            return new MediaStyleNotificationBuilder(context);
        } else if (applicationProperties.shouldUseBigNotifications()) {
            return new BigNotificationBuilder(context, remoteViewsFactory);
        } else if (applicationProperties.shouldUseRichNotifications()) {
            return new RichNotificationBuilder(context, remoteViewsFactory);
        } else {
            return new BasicNotificationBuilder(context);
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

    @SuppressWarnings("unchecked")
    @Provides
    public ImageProcessor provideImageProcessor(Context context) {
        return new ImageProcessorCompat();
    }

    @Provides
    public PopupMenuWrapper.Factory providePopupMenuWrapperFactory() {
        if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT) {
            return new PopupMenuWrapperICS.Factory();
        } else {
            return new PopupMenuWrapperCompat.Factory();
        }
    }

    @Provides
    @Singleton
    public CastConnectionHelper provideCastConnectionHelper(Context context, FeatureFlags featureFlags, ApplicationProperties applicationProperties){
        // The dalvik switch is a horrible hack to prevent instantiation of the real cast manager in unit tests as it crashes on robolectric.
        // This is temporary, until we play https://soundcloud.atlassian.net/browse/MC-213

        if (featureFlags.isEnabled(Feature.GOOGLE_CAST) && "Dalvik".equals(System.getProperty("java.vm.name"))){
            return new DefaultCastConnectionHelper(context, provideVideoCastManager(context, applicationProperties));
        } else {
            return new UselessCastConnectionHelper();
        }
    }

    @Provides
    @Singleton
    public VideoCastManager provideVideoCastManager(Context context, ApplicationProperties applicationProperties){
        return VideoCastManager.initialize(context, applicationProperties.getCastReceiverAppId(), null, "urn:x-cast:com.soundcloud.cast.sender");
    }
}
