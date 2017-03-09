package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.facebook.FacebookSdk;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.soundcloud.android.accounts.FacebookModule;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.firebase.FirebaseModule;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastModule;
import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.comments.CommentsModule;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.discovery.DiscoveryModule;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.image.ImageProcessorJB;
import com.soundcloud.android.main.NavigationModel;
import com.soundcloud.android.main.NavigationModelFactory;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineModule;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.CastPlaybackStrategy;
import com.soundcloud.android.playback.DefaultPlaybackStrategy;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackStrategy;
import com.soundcloud.android.playback.PlayerModule;
import com.soundcloud.android.playback.playqueue.PlayQueueModule;
import com.soundcloud.android.playback.ui.CompatLikeButtonPresenter;
import com.soundcloud.android.playback.ui.LikeButtonPresenter;
import com.soundcloud.android.playback.ui.MaterialLikeButtonPresenter;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.profile.ProfileModule;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.sync.SyncModule;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.rx.eventbus.DefaultEventBus;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LruCache;
import android.telephony.TelephonyManager;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.Executors;

@Module(
        includes = {
                FacebookModule.class,
                SyncModule.class,
                PlayerModule.class,
                PlaylistsModule.class,
                ProfileModule.class,
                CommentsModule.class,
                OfflineModule.class,
                DiscoveryModule.class,
                CastModule.class,
                PlayQueueModule.class,
                FirebaseModule.class
        })
public class ApplicationModule {

    public static final String HIGH_PRIORITY = "HighPriority";
    public static final String LOW_PRIORITY = "LowPriority";
    public static final String MAIN_LOOPER = "MainLooper";
    public static final String BUG_REPORTER = "BugReporter";
    public static final String CURRENT_DATE_PROVIDER = "CurrentDateProvider";
    public static final String DEFAULT_LIST_PAGE_SIZE = "DefaultListPageSize";

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
    public NavigationModel navigationModel(NavigationModelFactory navigationModelFactory) {
        return navigationModelFactory.build();
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
    @Singleton
    public FirebaseRemoteConfig provideFirebaseRemoteConfig(ApplicationProperties applicationProperties) {
        final FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(applicationProperties.isDevelopmentMode())
                .build();

        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setConfigSettings(remoteConfigSettings);

        return remoteConfig;
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
    public PlaybackStrategy providePlaybackStrategy(PlaybackServiceController serviceController,
                                                    CastConnectionHelper castConnectionHelper,
                                                    PlayQueueManager playQueueManager,
                                                    TrackItemRepository trackItemRepository,
                                                    Lazy<CastPlayer> castPlayer,
                                                    OfflinePlaybackOperations offlinePlaybackOperations,
                                                    PlaySessionStateProvider playSessionStateProvider,
                                                    EventBus eventBus) {
        if (castConnectionHelper.isCasting()) {
            return new CastPlaybackStrategy(castPlayer.get());
        } else {
            return new DefaultPlaybackStrategy(playQueueManager,
                                               serviceController,
                                               trackItemRepository,
                                               offlinePlaybackOperations,
                                               playSessionStateProvider,
                                               eventBus);
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
    @Singleton
    @Named(BUG_REPORTER)
    protected Scheduler provideBugReporterExecutor() {
        return Schedulers.from(Executors.newSingleThreadExecutor(r -> new Thread(r, "bugReporterThread")));
    }

    @Provides
    public FacebookSdk provideFacebookSdk() {
        return new FacebookSdk();
    }

    @Provides
    public Navigator provideNavigator(EventTracker eventTracker, FeatureFlags featureFlags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new SmoothNavigator(eventTracker, featureFlags);
        } else {
            return new Navigator(eventTracker, featureFlags);
        }
    }

    @Provides
    @Singleton
    public FabricReporter provideFabricReporter() {
        return new FabricReporter();
    }

    @Provides
    public LikeButtonPresenter provideLikeButtonPresenter(CondensedNumberFormatter numberFormatter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new MaterialLikeButtonPresenter(numberFormatter);
        } else {
            return new CompatLikeButtonPresenter(numberFormatter);
        }
    }

    @Provides
    @Named(CURRENT_DATE_PROVIDER)
    public DateProvider provideCurrentDateProvider() {
        return new CurrentDateProvider();
    }

    @Provides
    @Named(DEFAULT_LIST_PAGE_SIZE)
    public int provideDefaultListPageSize() {
        return Consts.LIST_PAGE_SIZE;
    }

}
