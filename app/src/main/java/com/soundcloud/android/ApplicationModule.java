package com.soundcloud.android;

import static com.soundcloud.android.waveform.WaveformOperations.DEFAULT_WAVEFORM_CACHE_SIZE;

import com.facebook.FacebookSdk;
import com.soundcloud.android.accounts.FacebookModule;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.crashlytics.FabricReportingHelper;
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
import com.soundcloud.android.crypto.Obfuscator;
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
import com.soundcloud.android.navigation.NavigationStateController;
import com.soundcloud.android.navigation.SmoothNavigationExecutor;
import com.soundcloud.android.offline.OfflineModule;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.onboarding.AuthSignature;
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
import com.soundcloud.android.properties.ApplicationProperties;
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

    protected ApplicationModule(SoundCloudApplication application) {
        this.application = application;
    }

    @Provides
    SoundCloudApplication provideApplication() {
        return application;
    }

    @Provides
    Context provideContext() {
        return application;
    }

    @Provides
    Resources provideResources() {
        return application.getResources();
    }

    @Provides
    @Singleton
    static NavigationModel navigationModel(NavigationModelFactory navigationModelFactory) {
        return navigationModelFactory.build();
    }

    @Provides
    CondensedNumberFormatter provideNumberFormatter() {
        return CondensedNumberFormatter.create(Locale.getDefault(), application.getResources());
    }

    @Provides
    AccountManager provideAccountManager() {
        return AccountManager.get(application);
    }

    @Provides
    SharedPreferences provideDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    ConnectivityManager provideConnectivityManager() {
        return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // EventBus is just required for the test implementation
    @Provides
    @Singleton
    protected ConnectionHelper provideConnectionHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager, EventBus eventBus) {
        return new NetworkConnectionHelper(connectivityManager, telephonyManager);
    }

    @Provides
    protected AuthSignature provideAuthSignature(Obfuscator obfuscator) {
        return new AuthSignature(obfuscator);
    }

    @Provides
    protected ConfigurationOperations provideConfigurationOperations(ApiClientRx apiClientRx,
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
    AlarmManager provideAlarmManager() {
        return (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
    }

    @Provides
    TelephonyManager provideTelephonyManager() {
        return (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Provides
    NotificationManager provideNotificationManager() {
        return (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    LocalBroadcastManager provideLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(application);
    }

    @Provides
    @Singleton
    static EventBus provideEventBus() {
        return new DefaultEventBus(AndroidSchedulers.mainThread());
    }

    @Provides
    @Singleton
    static EventBusV2 provideEventBusV2(EventBus eventBus) {
        return new DefaultEventBusV2(io.reactivex.android.schedulers.AndroidSchedulers.mainThread(), eventBus);
    }

    @Provides
    @Named(MAIN_LOOPER)
    static Looper providesMainLooper() {
        return Looper.getMainLooper();
    }

    @Provides
    SoundRecorder provideSoundRecorder() {
        return SoundRecorder.getInstance(application);
    }

    @Provides
    static AppWidgetManager provideAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    @Provides
    static NotificationCompat.Builder provideNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context);
    }

    @Singleton
    @Provides
    static LruCache<Urn, WaveformData> provideWaveformCache() {
        return new LruCache<>(DEFAULT_WAVEFORM_CACHE_SIZE);
    }

    @Provides
    static ImageProcessor provideImageProcessor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new ImageProcessorJB(context);
        } else {
            return new ImageProcessorCompat();
        }
    }

    @Provides
    static MainNavigationView provideNavigationView(AppNavigationExperiment appNavigationExperiment,
                                                    EnterScreenDispatcher enterScreenDispatcher,
                                                    NavigationModel navigationModel,
                                                    EventTracker eventTracker,
                                                    IntroductoryOverlayPresenter introductoryOverlayPresenter,
                                                    NavigationStateController navigationStateController) {
        if (appNavigationExperiment.isBottomNavigationEnabled()) {
            return new MainNavigationViewBottom(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter, navigationStateController);
        } else {
            return new MainNavigationViewTabs(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
        }
    }

    @Provides
    static BottomNavigationViewPresenter provideBottomNavigationViewPresenter(AppNavigationExperiment appNavigationExperiment,
                                                                              NavigationModel navigationModel,
                                                                              NavigationStateController navigationStateController) {
        if (appNavigationExperiment.isBottomNavigationEnabled()) {
            return new BottomNavigationViewPresenter.Default(navigationModel, navigationStateController);
        } else {
            return new BottomNavigationViewPresenter.Noop();
        }
    }

    @Provides
    static PlaybackStrategy providePlaybackStrategy(PlaybackServiceController serviceController,
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
    WifiManager provideWifiManager() {
        return (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
    }

    @Provides
    PowerManager providePowerManager() {
        return (PowerManager) application.getSystemService(Context.POWER_SERVICE);
    }

    @Provides
    @Named(HIGH_PRIORITY)
    static rx.Scheduler provideHighPriorityScheduler() {
        return ScSchedulers.HIGH_PRIO_SCHEDULER;
    }

    @Provides
    @Named(LOW_PRIORITY)
    static rx.Scheduler provideLowPriorityScheduler() {
        return ScSchedulers.LOW_PRIO_SCHEDULER;
    }

    @Provides
    @Named(RX_HIGH_PRIORITY)
    static Scheduler provideHighPriorityRxScheduler() {
        return ScSchedulers.RX_HIGH_PRIORITY_SCHEDULER;
    }

    @Provides
    @Named(RX_LOW_PRIORITY)
    static Scheduler provideLowPriorityRxScheduler() {
        return ScSchedulers.RX_LOW_PRIORITY_SCHEDULER;
    }

    @Provides
    @Singleton
    @Named(BUG_REPORTER)
    protected Scheduler provideBugReporterExecutor() {
        return Schedulers.from(Executors.newSingleThreadExecutor(r -> new Thread(r, "bugReporterThread")));
    }

    @Provides
    static FacebookSdk provideFacebookSdk() {
        return new FacebookSdk();
    }

    @SuppressLint("VisibleForTests")
    @Provides
    static NavigationExecutor provideNavigationExecutor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new SmoothNavigationExecutor();
        } else {
            return new NavigationExecutor();
        }
    }

    @Provides
    @Singleton
    static FabricReporter provideFabricReporter() {
        return new FabricReporter();
    }

    @Provides
    static LikeButtonPresenter provideLikeButtonPresenter(CondensedNumberFormatter numberFormatter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new MaterialLikeButtonPresenter(numberFormatter);
        } else {
            return new CompatLikeButtonPresenter(numberFormatter);
        }
    }

    @Provides
    @Named(CURRENT_DATE_PROVIDER)
    static DateProvider provideCurrentDateProvider() {
        return new CurrentDateProvider();
    }

    @Provides
    @Named(DEFAULT_LIST_PAGE_SIZE)
    static int provideDefaultListPageSize() {
        return Consts.LIST_PAGE_SIZE;
    }

    @Provides
    @Named(ENRICHED_ENTITY_ITEM_EMITTER)
    static EntityItemEmitter provideEntichedEntityItemEmitter(EntityItemCreator entityItemCreator,
                                                              LikesStateProvider likeStateProvider,
                                                              RepostsStateProvider repostsStateProvider,
                                                              PlaySessionStateProvider playSessionStateProvider,
                                                              FollowingStateProvider followingStateProvider) {
        return new EnrichedEntities(entityItemCreator, likeStateProvider, repostsStateProvider, playSessionStateProvider, followingStateProvider);
    }

    @Singleton
    @Provides
    protected GooglePlayServicesWrapper provideGooglePlayServicesWrapper() {
        return new GooglePlayServicesWrapper();
    }

    @Provides
    static SearchItemRenderer<DiscoveryCardViewModel> provideNewSearchItemRenderer() {
        return new SearchItemRenderer<>();
    }

    @Provides
    static Random provideRandom() {
        return new Random();
    }

    @Provides
    FabricReportingHelper provideFabricReportingHelper(ApplicationProperties applicationProperties, SharedPreferences sharedPreferences) {
        return new FabricReportingHelper(applicationProperties, sharedPreferences);
    }
}
