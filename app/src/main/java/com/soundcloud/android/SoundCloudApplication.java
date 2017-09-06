package com.soundcloud.android;

import static com.soundcloud.android.analytics.performance.MetricType.DEV_APP_ON_CREATE;
import static com.soundcloud.android.rx.observers.LambdaMaybeObserver.onNext;

import com.facebook.stetho.Stetho;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.perf.FirebasePerformance;
import com.moat.analytics.mobile.scl.MoatAnalytics;
import com.moat.analytics.mobile.scl.MoatOptions;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.SessionProvider;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.ads.PlayerAdsControllerProxy;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.analytics.PlaySessionOriginScreenProvider;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.analytics.crashlytics.FabricProvider;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.cast.DefaultCastSessionController;
import com.soundcloud.android.collection.playhistory.PlayHistoryController;
import com.soundcloud.android.configuration.ConfigurationFeatureController;
import com.soundcloud.android.configuration.ForceUpdateHandler;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.ApplicationStartupMeterFactory;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineStorageOperations;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.peripherals.PeripheralsControllerProxy;
import com.soundcloud.android.playback.PlayPublisherProxy;
import com.soundcloud.android.playback.PlayQueueExtenderProxy;
import com.soundcloud.android.playback.PlaySessionControllerProxy;
import com.soundcloud.android.playback.PlaybackMeter;
import com.soundcloud.android.playback.PlaylistExploderProxy;
import com.soundcloud.android.playback.StreamPreloader;
import com.soundcloud.android.playback.skippy.SkippyFactory;
import com.soundcloud.android.playback.widget.PlayerWidgetControllerProxy;
import com.soundcloud.android.policies.DailyUpdateScheduler;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.startup.migrations.MigrationEngine;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsController;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.DatabaseCleanupScheduler;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.annotations.VisibleForTesting;
import com.squareup.leakcanary.LeakCanary;
import dagger.Lazy;
import io.reactivex.plugins.RxJavaPlugins;
import org.jetbrains.annotations.NotNull;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;
import java.io.InterruptedIOException;

public class SoundCloudApplication extends MultiDexApplication {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    // Performance: we want to start timing when the class loader loads classes.
    private final PerformanceMetric appOnCreateMetric = PerformanceMetric.create(DEV_APP_ON_CREATE);

    private static SoundCloudApplication instance;

    // These are not injected because we need them before Dagger initializes
    private UncaughtExceptionHandlerController uncaughtExceptionHandlerController;
    private SharedPreferences sharedPreferences;
    private ApplicationProperties applicationProperties;

    @Inject MigrationEngine migrationEngine;
    @Inject NetworkConnectivityListener networkConnectivityListener;
    @Inject SessionProvider sessionProvider;
    @Inject AccountOperations accountOperations;
    @Inject ForceUpdateHandler forceUpdateHandler;
    @Inject PlayerWidgetControllerProxy widgetControllerListener;
    @Inject PeripheralsControllerProxy peripheralsControllerProxy;
    @Inject PlaySessionControllerProxy playSessionControllerProxy;
    @Inject PlaylistExploderProxy playlistExploderProxy;
    @Inject PlayQueueExtenderProxy playQueueExtenderProxy;
    @Inject PlayPublisherProxy playPublisherProxy;
    @Inject PlayerAdsControllerProxy playerAdsControllerProxy;
    @Inject SkippyFactory skippyFactory;
    @Inject FeatureFlags featureFlags;
    @Inject CryptoOperations cryptoOperations;
    @Inject ConfigurationFeatureController configurationFeatureController;
    @Inject ScreenProvider screenProvider;
    @Inject PlaySessionOriginScreenProvider playSessionOriginScreenProvider;
    @Inject AdIdHelper adIdHelper;
    @Inject Lazy<DefaultCastSessionController> castControllerProvider;
    @Inject StationsController stationsController;
    @Inject DailyUpdateScheduler dailyUpdateScheduler;
    @Inject DatabaseCleanupScheduler databaseCleanupScheduler;
    @Inject AppboyPlaySessionState appboyPlaySessionState;
    @Inject StreamPreloader streamPreloader;
    @Inject TrackOfflineStateProvider trackOfflineStateProvider;
    @Inject OfflinePropertiesProvider offlinePropertiesProvider;
    @Inject SyncConfig syncConfig;
    @Inject PlayHistoryController playHistoryController;
    @Inject SyncInitiator syncInitiator;
    @Inject StationsOperations stationsOperations;
    @Inject GooglePlayServicesWrapper googlePlayServicesWrapper;
    @Inject LikesStateProvider likesStateProvider;
    @Inject RepostsStateProvider repostsStateProvider;
    @Inject FollowingStateProvider followingStateProvider;
    @Inject PerformanceMetricsEngine performanceMetricsEngine;
    @Inject ApplicationStartupMeterFactory applicationStartupMeterFactory;
    @Inject PlaybackMeter playbackMeter;
    @Inject OfflineStorageOperations offlineStorageOperations;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    // we need this object to exist throughout the life time of the app,
    // even if it appears to be unused
    @Inject @SuppressWarnings("unused") AnalyticsEngine analyticsEngine;

    private ApplicationComponent applicationComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.applicationComponent = buildApplicationComponent();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initializeFirebase();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }

        instance = this;

        initializePreInjectionObjects();
        setUpCrashReportingIfNeeded();
        setupRxErrorHandling();

        applicationComponent.inject(this);

        if (applicationProperties.isDevelopmentMode()) {
            leakCanaryWrapper.install(this);
            Stetho.initializeWithDefaults(this);
        }

        if (applicationProperties.isBetaBuild()) {
            // https://www.fabric.io/soundcloudandroid/android/apps/com.soundcloud.android/issues/5838cd8c0aeb16625b4a6c86
            FragmentManager.enableDebugLogging(true);
        }


        setupPerformanceMonitoring();
        bootApplication();

        performanceMetricsEngine.endMeasuringFrom(appOnCreateMetric);
    }

    private void initializeFirebase() {
        // we do this manually, so that we can support LeakCanary :
        // http://stackoverflow.com/questions/37585090/leakcanary-crashes-with-googles-firebase
        final FirebaseOptions options = FirebaseOptions.fromResource(this);
        FirebaseApp.initializeApp(this, options);
    }

    private void setupPerformanceMonitoring() {
        FirebasePerformance.getInstance().setPerformanceCollectionEnabled(featureFlags.isEnabled(Flag.FIREBASE_PERFORMANCE_MONITORING));
    }

    protected ApplicationComponent buildApplicationComponent() {
        return DaggerApplicationComponent.builder()
                                         .applicationModule(new ApplicationModule(this))
                                         .build();
    }

    protected void bootApplication() {
        migrationEngine.migrate();

        Log.i(TAG, "Application starting up in mode " + applicationProperties.getBuildTypeName());
        Log.d(TAG, applicationProperties.toString());

        if (applicationProperties.isDevBuildRunningOnDevice() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
            Log.i(TAG, DeviceHelper.getBuildInfo());
        }

        setupMoatAnalytics();

        adIdHelper.init();

        uncaughtExceptionHandlerController.reportSystemMemoryStats();

        analyticsEngine.onAppCreated(this);

        // initialise skippy so it can do it's expensive one-shot ops
        skippyFactory.create().preload();

        setupCurrentUserAccount();

        offlineStorageOperations.init();
        cryptoOperations.generateAndStoreDeviceKeyIfNeeded();
        networkConnectivityListener.startListening();
        widgetControllerListener.subscribe();
        peripheralsControllerProxy.subscribe();
        playSessionControllerProxy.subscribe();
        playerAdsControllerProxy.subscribe();
        screenProvider.subscribe();
        playSessionOriginScreenProvider.subscribe();
        appboyPlaySessionState.subscribe();
        applicationStartupMeterFactory.create(this).subscribe();
        playbackMeter.subscribe();

        if (googlePlayServicesWrapper.isPlayServiceAvailable(this)) {
            castControllerProvider.get().startListening();
        }

        trackOfflineStateProvider.subscribe();
        playQueueExtenderProxy.subscribe();
        playHistoryController.subscribe();
        playlistExploderProxy.subscribe();

        if (applicationProperties.enforceConcurrentStreamingLimitation()) {
            playPublisherProxy.subscribe();
        }

        stationsController.subscribe();
        dailyUpdateScheduler.schedule();
        databaseCleanupScheduler.schedule();
        streamPreloader.subscribe();

        configurationFeatureController.subscribe();
        likesStateProvider.subscribe();
        followingStateProvider.subscribe();

        repostsStateProvider.subscribe();
        offlinePropertiesProvider.subscribe();

        uncaughtExceptionHandlerController.assertHandlerIsSet();

        forceUpdateHandler.checkPendingForcedUpdate();
    }

    private void initializePreInjectionObjects() {
        applicationProperties = new ApplicationProperties(getResources());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        uncaughtExceptionHandlerController = new UncaughtExceptionHandlerController(this, isReportingCrashes());
    }

    private void setUpCrashReportingIfNeeded() {
        if (isReportingCrashes()) {
            FabricProvider.initialize(this);
        }
        uncaughtExceptionHandlerController.setHandler();
    }

    private void setupMoatAnalytics() {
        final MoatOptions options = new MoatOptions();
        options.disableAdIdCollection = true;
        MoatAnalytics.getInstance().start(options, this);
        MoatAnalytics.getInstance().prepareNativeDisplayTracking(getString(R.string.moat_display_partner_id));
    }

    private void setupCurrentUserAccount() {
        sessionProvider.currentAccount()
                       .subscribeOn(ScSchedulers.RX_HIGH_PRIORITY_SCHEDULER)
                       .filter(syncConfig::isSyncingEnabled)
                       .subscribeWith(onNext(syncConfig::enableSyncing));
    }

    @NotNull
    public static ApplicationComponent getObjectGraph() {
        if (instance == null || instance.applicationComponent == null) {
            throw new IllegalStateException(
                    "Cannot access the app graph before the application has been created");
        }
        return instance.applicationComponent;
    }

    /**
     * Make sure that sets are synced first, to avoid running into data consistency issues around adding tracks
     * to playlists, see https://github.com/soundcloud/android-listeners/issues/609
     * <p/>
     * Alternatively, sync sets lazily where needed.
     */
    private void requestCollectionsSync() {
        syncInitiator.syncAndForget(Syncable.MY_PLAYLISTS);
        syncInitiator.syncAndForget(Syncable.PLAYLIST_LIKES);
        syncInitiator.syncAndForget(Syncable.TRACK_LIKES);
        stationsOperations.syncStations(StationsCollectionsTypes.LIKED).toCompletable().subscribe(new DefaultDisposableCompletableObserver());
        syncInitiator.syncAndForget(Syncable.PLAY_HISTORY);
        syncInitiator.syncAndForget(Syncable.RECENTLY_PLAYED);
    }

    private void setupStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                                           .detectAll()
                                           .penaltyLog()
                                           .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                       .detectAll()
                                       .penaltyLog()
                                       .build());
    }

    // If an item is added to an Observer that has been disposed, RxJava2 will propagate the error to the
    // Uncaught Exception Handler. By default this crashes our app. These errors happen when we don't
    // properly wrap a non-RX api with an Rx Api. For example, converting our non-Rx API calls into
    // Observables / Singles.
    private void setupRxErrorHandling() {
        RxJavaPlugins.setErrorHandler(e -> {
            // Some blocking code was interrupted by a dispose call
            // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
            if (!applicationProperties.isDevelopmentMode()) {
                ErrorUtils.handleSilentException("RxError", e);
            } else {
                handleThrowableInDebug(e);
            }
        });
    }

    private void handleThrowableInDebug(Throwable t) {
        //We don't want to crash the app in debug when we unsubscribe from RX chain
        if (t.getCause() != null && t.getCause().getCause() != null && t.getCause().getCause() instanceof InterruptedIOException) {
            //Ignore interrupted cause
            Log.d(TAG, "Expected interrupted IO exception");
            Log.d(TAG, t.toString());
        } else {
            throw new RuntimeException("Debug exception from Rx chain", t);
        }
    }

    private boolean isReportingCrashes() {
        return applicationProperties.shouldReportCrashes() &&
                sharedPreferences.getBoolean(SettingKey.CRASH_REPORTING_ENABLED, true);
    }

    @Override
    public void onLowMemory() {
        onTrimMemory(TRIM_MEMORY_COMPLETE);
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if (!LeakCanary.isInAnalyzerProcess(this)) {
            uncaughtExceptionHandlerController.reportMemoryTrim(level);
        }
        super.onTrimMemory(level);
    }

    @VisibleForTesting
    public boolean addUserAccountAndEnableSync(ApiUser user, Token token, SignupVia via) {
        Account account = accountOperations.addOrReplaceSoundCloudAccount(user, token, via);
        if (account != null) {
            // move this when we can't guarantee we will only have 1 account active at a time
            syncConfig.enableSyncing(account);
            requestCollectionsSync();
            return true;
        } else {
            return false;
        }
    }
}
