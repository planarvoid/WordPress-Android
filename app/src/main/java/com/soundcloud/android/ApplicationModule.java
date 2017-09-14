package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.facebook.FacebookSdk;
import com.soundcloud.android.accounts.FacebookModule;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.firebase.FirebaseModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastModule;
import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.comments.CommentsModule;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.ConfigurationSettingsStorage;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.ForceUpdateHandler;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.PlanChangeDetector;
import com.soundcloud.android.configuration.experiments.AppNavigationExperiment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.discovery.DiscoveryCardViewModel;
import com.soundcloud.android.image.ImageConfigurationStorage;
import com.soundcloud.android.image.ImageModule;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.image.ImageProcessorJB;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.MainNavigationView;
import com.soundcloud.android.main.MainNavigationViewBottom;
import com.soundcloud.android.main.MainNavigationViewTabs;
import com.soundcloud.android.main.NavigationModel;
import com.soundcloud.android.main.NavigationModelFactory;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.BottomNavigationViewPresenter;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.SmoothNavigationExecutor;
import com.soundcloud.android.offline.OfflineModule;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.playback.CastPlaybackStrategy;
import com.soundcloud.android.playback.DefaultPlaybackStrategy;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackStrategy;
import com.soundcloud.android.playback.PlayerUIModule;
import com.soundcloud.android.playback.playqueue.PlayQueueModule;
import com.soundcloud.android.playback.ui.CompatLikeButtonPresenter;
import com.soundcloud.android.playback.ui.LikeButtonPresenter;
import com.soundcloud.android.playback.ui.MaterialLikeButtonPresenter;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.presentation.EnrichedEntities;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.EntityItemEmitter;
import com.soundcloud.android.profile.ProfileModule;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.sync.SyncModule;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.TryWithBackOff;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.rx.eventbus.DefaultEventBus;
import com.soundcloud.rx.eventbus.DefaultEventBusV2;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import rx.android.schedulers.AndroidSchedulers;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
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
import java.util.Random;
import java.util.concurrent.Executors;

@Module(
        includes = {
                FacebookModule.class,
                SyncModule.class,
                PlayerUIModule.class,
                PlaylistsModule.class,
                ProfileModule.class,
                CommentsModule.class,
                OfflineModule.class,
                CastModule.class,
                PlayQueueModule.class,
                FirebaseModule.class,
                ImageModule.class
        })
public class ApplicationModule {

    @Deprecated
    /** Use {@link ApplicationModule#RX_HIGH_PRIORITY}. */
    public static final String HIGH_PRIORITY = "HighPriority";
    @Deprecated
    /** Use {@link ApplicationModule#RX_LOW_PRIORITY}. */
    public static final String LOW_PRIORITY = "LowPriority";

    public static final String RX_HIGH_PRIORITY = "RxHighPriority";
    public static final String RX_LOW_PRIORITY = "RxLowPriority";
    public static final String MAIN_LOOPER = "MainLooper";
    public static final String BUG_REPORTER = "BugReporter";
    public static final String CURRENT_DATE_PROVIDER = "CurrentDateProvider";
    public static final String DEFAULT_LIST_PAGE_SIZE = "DefaultListPageSize";

    public static final String ENRICHED_ENTITY_ITEM_EMITTER = "EnrichedEntityItemEmitter";

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
    public ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // EventBus is just required for the test implementation
    @Provides
    @Singleton
    public ConnectionHelper provideConnectionHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager, EventBus eventBus) {
        return new NetworkConnectionHelper(connectivityManager, telephonyManager);
    }

    @Provides
    public ConfigurationOperations provideConfigurationOperations(ApiClientRx apiClientRx,
                                                                  ExperimentOperations experimentOperations,
                                                                  FeatureOperations featureOperations,
                                                                  PendingPlanOperations pendingPlanOperations,
                                                                  ConfigurationSettingsStorage configurationSettingsStorage,
                                                                  TryWithBackOff.Factory tryWithBackOffFactory,
                                                                  @Named(HIGH_PRIORITY) rx.Scheduler scheduler,
                                                                  PlanChangeDetector planChangeDetector,
                                                                  ForceUpdateHandler forceUpdateHandler,
                                                                  ImageConfigurationStorage imageConfigurationStorage) {
        return new ConfigurationOperations(apiClientRx,
                                           experimentOperations,
                                           featureOperations,
                                           pendingPlanOperations,
                                           configurationSettingsStorage,
                                           tryWithBackOffFactory,
                                           scheduler,
                                           planChangeDetector,
                                           forceUpdateHandler,
                                           imageConfigurationStorage);
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
    @Singleton
    public EventBusV2 provideEventBusV2(EventBus eventBus) {
        return new DefaultEventBusV2(io.reactivex.android.schedulers.AndroidSchedulers.mainThread(), eventBus);
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
    public MainNavigationView provideNavigationView(AppNavigationExperiment appNavigationExperiment,
                                                    EnterScreenDispatcher enterScreenDispatcher,
                                                    NavigationModel navigationModel,
                                                    EventTracker eventTracker,
                                                    IntroductoryOverlayPresenter introductoryOverlayPresenter) {
        if (appNavigationExperiment.isBottomNavigationEnabled()) {
            return new MainNavigationViewBottom(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        } else {
            return new MainNavigationViewTabs(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        }
    }

    @Provides
    public BottomNavigationViewPresenter provideBottomNavigationViewPresenter(AppNavigationExperiment appNavigationExperiment,
                                                                              NavigationModel navigationModel) {
        if (appNavigationExperiment.isBottomNavigationEnabled()) {
            return new BottomNavigationViewPresenter.Default(navigationModel);
        } else {
            return new BottomNavigationViewPresenter.Noop();
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
                                                    EventBusV2 eventBus,
                                                    OfflineSettingsStorage offlineSettingsStorage,
                                                    FeedbackController feedbackController) {
        if (castConnectionHelper.isCasting()) {
            return new CastPlaybackStrategy(playQueueManager, castPlayer.get());
        } else {
            return new DefaultPlaybackStrategy(playQueueManager,
                                               serviceController,
                                               trackItemRepository,
                                               offlinePlaybackOperations,
                                               playSessionStateProvider,
                                               eventBus,
                                               offlineSettingsStorage,
                                               feedbackController);
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
    public rx.Scheduler provideHighPriorityScheduler() {
        return ScSchedulers.HIGH_PRIO_SCHEDULER;
    }

    @Provides
    @Named(LOW_PRIORITY)
    public rx.Scheduler provideLowPriorityScheduler() {
        return ScSchedulers.LOW_PRIO_SCHEDULER;
    }

    @Provides
    @Named(RX_HIGH_PRIORITY)
    public Scheduler provideHighPriorityRxScheduler() {
        return ScSchedulers.RX_HIGH_PRIORITY_SCHEDULER;
    }

    @Provides
    @Named(RX_LOW_PRIORITY)
    public Scheduler provideLowPriorityRxScheduler() {
        return ScSchedulers.RX_LOW_PRIORITY_SCHEDULER;
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

    @SuppressLint("VisibleForTests")
    @Provides
    public NavigationExecutor provideNavigationExecutor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new SmoothNavigationExecutor();
        } else {
            return new NavigationExecutor();
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

    @Provides
    @Named(ENRICHED_ENTITY_ITEM_EMITTER)
    public EntityItemEmitter provideEntichedEntityItemEmitter(EntityItemCreator entityItemCreator,
                                                              LikesStateProvider likeStateProvider,
                                                              RepostsStateProvider repostsStateProvider,
                                                              PlaySessionStateProvider playSessionStateProvider,
                                                              FollowingStateProvider followingStateProvider) {
        return new EnrichedEntities(entityItemCreator, likeStateProvider, repostsStateProvider, playSessionStateProvider, followingStateProvider);
    }

    @Singleton
    @Provides
    public GooglePlayServicesWrapper provideGooglePlayServicesWrapper() {
        return new GooglePlayServicesWrapper();
    }

    @Provides
    public SearchItemRenderer<DiscoveryCardViewModel> provideNewSearchItemRenderer() {
        return new SearchItemRenderer<>();
    }

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
