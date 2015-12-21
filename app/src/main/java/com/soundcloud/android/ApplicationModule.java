package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.appboy.Appboy;
import com.facebook.FacebookSdk;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.cast.DefaultCastConnectionHelper;
import com.soundcloud.android.cast.NoOpCastConnectionHelper;
import com.soundcloud.android.collections.CollectionNavigationTarget;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.image.ImageProcessorJB;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.NavigationModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.CastPlaybackStrategy;
import com.soundcloud.android.playback.DefaultPlaybackStrategy;
import com.soundcloud.android.playback.FallbackRemoteAudioManager;
import com.soundcloud.android.playback.IRemoteAudioManager;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackStrategy;
import com.soundcloud.android.playback.RemoteAudioManager;
import com.soundcloud.android.playback.notification.BigNotificationBuilder;
import com.soundcloud.android.playback.notification.MediaStyleNotificationBuilder;
import com.soundcloud.android.playback.notification.NotificationBuilder;
import com.soundcloud.android.playback.notification.RichNotificationBuilder;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.search.DiscoveryNavigationTarget;
import com.soundcloud.android.stations.StationsNavigationTarget;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.stream.StreamNavigationTarget;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.you.YouNavigationTarget;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.rx.eventbus.DefaultEventBus;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.telephony.TelephonyManager;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;

@Module(library = true, includes = {ApiModule.class, StorageModule.class})
public class ApplicationModule {

    public static final String HIGH_PRIORITY = "HighPriority";
    public static final String LOW_PRIORITY = "LowPriority";
    public static final String MAIN_LOOPER = "MainLooper";

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
    @Singleton
    public NavigationModel navigationModel(FeatureFlags featureFlags) {
        if (featureFlags.isEnabled(Flag.STATIONS_HOME)) {
            return new NavigationModel(
                    new StreamNavigationTarget(),
                    new StationsNavigationTarget(),
                    new DiscoveryNavigationTarget(),
                    new CollectionNavigationTarget(),
                    new YouNavigationTarget());
        } else {
            return new NavigationModel(
                    new StreamNavigationTarget(),
                    new DiscoveryNavigationTarget(),
                    new CollectionNavigationTarget(),
                    new YouNavigationTarget());
        }
    }

    @Provides
    public CondensedNumberFormatter provideNumberFormatter() {
        return CondensedNumberFormatter.create(Locale.getDefault(), application.getResources());
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
    public AlarmManager provideAlarmManager() {
        return (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
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
        return new DefaultEventBus(AndroidSchedulers.mainThread());
    }

    @Provides
    @Named(MAIN_LOOPER)
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
    public NotificationBuilder providesNotificationBuilderWrapper(Context context,
                                                                  ApplicationProperties applicationProperties,
                                                                  NotificationPlaybackRemoteViews.Factory remoteViewsFactory,
                                                                  ImageOperations imageOperations) {
        if (applicationProperties.shouldUseMediaStyleNotifications()) {
            return new MediaStyleNotificationBuilder(context, imageOperations);
        } else if (applicationProperties.shouldUseBigNotifications()) {
            return new BigNotificationBuilder(context, remoteViewsFactory, imageOperations);
        } else {
            return new RichNotificationBuilder(context, remoteViewsFactory, imageOperations);
        }
    }

    @SuppressWarnings("unchecked")
    @Provides
    @Singleton
    public IRemoteAudioManager provideRemoteAudioManager(Context context) {
        try {
            return new RemoteAudioManager(context);
        } catch (Exception e) {
            ErrorUtils.handleSilentException("Could not create remote audio manager", e);
        }
        return new FallbackRemoteAudioManager(context);
    }

    @Singleton
    @Provides
    public LruCache<Urn, WaveformData> provideWaveformCache() {
        return new LruCache<>(DEFAULT_WAVEFORM_CACHE_SIZE);
    }

    @Provides
    public ImageProcessor provideImageProcessor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new ImageProcessorJB(context);
        } else {
            return new ImageProcessorCompat();
        }
    }

    @Provides
    @Singleton
    public CastConnectionHelper provideCastConnectionHelper(Context context, ApplicationProperties applicationProperties) {
        // The dalvik switch is a horrible hack to prevent instantiation of the real cast manager in unit tests as it crashes on robolectric.
        // This is temporary, until we play https://soundcloud.atlassian.net/browse/MC-213

        if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
            return new DefaultCastConnectionHelper(provideVideoCastManager(context, applicationProperties));
        } else {
            return new NoOpCastConnectionHelper();
        }
    }

    @Provides
    @Singleton
    public VideoCastManager provideVideoCastManager(Context context, ApplicationProperties applicationProperties) {
        final VideoCastManager manager = VideoCastManager.initialize(context, applicationProperties.getCastReceiverAppId(), MainActivity.class, "urn:x-cast:com.soundcloud.cast.sender");
        manager.enableFeatures(VideoCastManager.FEATURE_LOCKSCREEN | VideoCastManager.FEATURE_DEBUGGING |
                VideoCastManager.FEATURE_NOTIFICATION | VideoCastManager.FEATURE_WIFI_RECONNECT);
        return manager;
    }

    @Provides
    public PlaybackStrategy providePlaybackStrategy(ServiceInitiator serviceInitiator,
                                                    CastConnectionHelper castConnectionHelper,
                                                    PlayQueueManager playQueueManager,
                                                    Lazy<CastPlayer> castPlayer,
                                                    TrackRepository trackRepository,
                                                    AdsOperations adsOperations,
                                                    OfflinePlaybackOperations offlinePlaybackOperations,
                                                    PlaySessionStateProvider playSessionStateProvider,
                                                    EventBus eventBus) {
        if (castConnectionHelper.isCasting()) {
            return new CastPlaybackStrategy(castPlayer.get());
        } else {
            return new DefaultPlaybackStrategy(playQueueManager, serviceInitiator, trackRepository, adsOperations, offlinePlaybackOperations, playSessionStateProvider, eventBus);
        }
    }

    @Provides
    public WifiManager provideWifiManager() {
        return (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
    }

    @Provides
    public PowerManager providePowerManager() {
        return (PowerManager) application.getSystemService(Context.POWER_SERVICE);
    }

    @Provides
    @Named(HIGH_PRIORITY)
    public Scheduler provideHighPriorityScheduler() {
        return ScSchedulers.HIGH_PRIO_SCHEDULER;
    }

    @Provides
    @Named(LOW_PRIORITY)
    public Scheduler provideLowPriorityScheduler() {
        return ScSchedulers.LOW_PRIO_SCHEDULER;
    }

    @Provides
    public FacebookSdk provideFacebookSdk() {
        return new FacebookSdk();
    }

    @Provides
    @Singleton
    @Nullable
    public Appboy provideAppboy(Context context) {
        return Appboy.getInstance(context);
    }

    @Provides
    public Navigator provideNavigator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new SmoothNavigator();
        } else {
            return new Navigator();
        }
    }

    @Provides
    @Singleton
    public FabricReporter provideFabricReporter() {
        return new FabricReporter();
    }
}
