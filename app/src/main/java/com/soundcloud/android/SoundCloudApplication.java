package com.soundcloud.android;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.facebook.FacebookSdk;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.analytics.crashlytics.FabricProvider;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.cast.CastSessionController;
import com.soundcloud.android.collection.playhistory.PlayHistoryController;
import com.soundcloud.android.configuration.ConfigurationFeatureController;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.peripherals.PeripheralsController;
import com.soundcloud.android.playback.PlayPublisher;
import com.soundcloud.android.playback.PlayQueueExtender;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaylistExploder;
import com.soundcloud.android.playback.StreamPreloader;
import com.soundcloud.android.playback.skippy.SkippyFactory;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.policies.DailyUpdateScheduler;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.startup.migrations.MigrationEngine;
import com.soundcloud.android.stations.StationsController;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

public class SoundCloudApplication extends MultiDexApplication {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    // Remove these fields when we've moved to a full DI solution
    @Deprecated
    @SuppressFBWarnings({"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static SoundCloudApplication instance;

    // These are not injected because we need them before Dagger initializes
    private UncaughtExceptionHandlerController uncaughtExceptionHandlerController;
    private SharedPreferences sharedPreferences;
    private ApplicationProperties applicationProperties;

    @Inject DevToolsHelper devTools;
    @Inject MigrationEngine migrationEngine;
    @Inject EventBus eventBus;
    @Inject NetworkConnectivityListener networkConnectivityListener;
    @Inject ImageOperations imageOperations;
    @Inject AccountOperations accountOperations;
    @Inject ConfigurationManager configurationManager;
    @Inject PlayerWidgetController widgetController;
    @Inject PeripheralsController peripheralsController;
    @Inject PlaySessionController playSessionController;
    @Inject PlaySessionStateProvider playSessionStateProvider;
    @Inject PlaylistExploder playlistExploder;
    @Inject PlayQueueExtender playQueueExtender;
    @Inject PlayPublisher playPublisher;
    @Inject AdsController adsController;
    @Inject PlaylistTagStorage playlistTagStorage;
    @Inject SkippyFactory skippyFactory;
    @Inject FeatureFlags featureFlags;
    @Inject CryptoOperations cryptoOperations;
    @Inject ConfigurationFeatureController configurationFeatureController;
    @Inject ScreenProvider screenProvider;
    @Inject AdIdHelper adIdHelper;
    @Inject CastSessionController castSessionController;
    @Inject StationsController stationsController;
    @Inject DailyUpdateScheduler dailyUpdateScheduler;
    @Inject AppboyPlaySessionState appboyPlaySessionState;
    @Inject StreamPreloader streamPreloader;
    @Inject @Named(StorageModule.STREAM_CACHE_DIRECTORY) File streamCacheDirectory;
    @Inject TrackOfflineStateProvider trackOfflineStateProvider;
    @Inject SyncConfig syncConfig;
    @Inject PlayHistoryController playHistoryController;
    @Inject SyncInitiatorBridge syncInitiatorBridge;

    // we need this object to exist throughout the life time of the app,
    // even if it appears to be unused
    @Inject @SuppressWarnings("unused") AnalyticsEngine analyticsEngine;

    protected ApplicationComponent objectGraph;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        objectGraph = DaggerApplicationComponent.builder()
                                                .applicationModule(new ApplicationModule(this))
                                                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initializePreInjectionObjects();
        setUpCrashReportingIfNeeded();

        objectGraph.inject(this);
        devTools.initialize(this);
        bootApplication();
    }

    protected void bootApplication() {
        migrationEngine.migrate();

        Log.i(TAG, "Application starting up in mode " + applicationProperties.getBuildType());
        Log.d(TAG, applicationProperties.toString());

        if (applicationProperties.isDevBuildRunningOnDevice() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
            Log.i(TAG, DeviceHelper.getBuildInfo());
        }

        adIdHelper.init();

        uncaughtExceptionHandlerController.reportSystemMemoryStats();

        IOUtils.createCacheDirs(this, streamCacheDirectory);

        analyticsEngine.onAppCreated(this);

        // initialise skippy so it can do it's expensive one-shot ops
        skippyFactory.create().preload();

        imageOperations.initialise(this, applicationProperties);

        setupCurrentUserAccount();

        cryptoOperations.generateAndStoreDeviceKeyIfNeeded();
        networkConnectivityListener.startListening();
        widgetController.subscribe();
        peripheralsController.subscribe();
        playSessionController.subscribe();
        adsController.subscribe();
        screenProvider.subscribe();
        appboyPlaySessionState.subscribe();
        castSessionController.startListening();
        trackOfflineStateProvider.subscribe();
        playQueueExtender.subscribe();
        playHistoryController.subscribe();
        playlistExploder.subscribe();

        if (applicationProperties.enforceConcurrentStreamingLimitation()) {
            playPublisher.subscribe();
        }

        stationsController.subscribe();
        dailyUpdateScheduler.schedule();
        streamPreloader.subscribe();

        configurationFeatureController.subscribe();
        FacebookSdk.sdkInitialize(getApplicationContext());
        uncaughtExceptionHandlerController.assertHandlerIsSet();

        configurationManager.checkForForcedApplicationUpdate();
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

    private void setupCurrentUserAccount() {
        final Account account = accountOperations.getSoundCloudAccount();

        if (account != null) {
            if (!syncConfig.isSyncingEnabled(account)) {
                syncConfig.enableSyncing(account);
            }

            // remove device url so clients resubmit the registration request with
            // device identifier
            AndroidUtils.doOnce(this, "reset.c2dm.reg_id", new Runnable() {
                @Override
                public void run() {
                    sharedPreferences.edit().remove(Consts.PrefKeys.C2DM_DEVICE_URL).apply();
                }
            });
            // sync current sets
            AndroidUtils.doOnce(this, "request.sets.sync", new Runnable() {
                @Override
                public void run() {
                    requestCollectionsSync();
                }
            });
        }
    }

    @Deprecated // use @Inject instead!
    public EventBus getEventBus() {
        return eventBus;
    }

    @Deprecated // use @Inject instead!
    public ImageOperations getImageOperations() {
        return imageOperations;
    }

    @Deprecated // use @Inject instead!
    public AccountOperations getAccountOperations() {
        return accountOperations;
    }

    @NotNull
    public static ApplicationComponent getObjectGraph() {
        if (instance == null || instance.objectGraph == null) {
            throw new IllegalStateException(
                    "Cannot access the app graph before the application has been created");
        }
        return instance.objectGraph;
    }

    /**
     * Make sure that sets are synced first, to avoid running into data consistency issues around adding tracks
     * to playlists, see https://github.com/soundcloud/SoundCloud-Android/issues/609
     * <p/>
     * Alternatively, sync sets lazily where needed.
     */
    private void requestCollectionsSync() {
        fireAndForget(syncInitiatorBridge.refreshMyPostedAndLikedPlaylists());
        fireAndForget(syncInitiatorBridge.refreshLikedTracks());
        fireAndForget(syncInitiatorBridge.refreshStations());
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
        uncaughtExceptionHandlerController.reportMemoryTrim(level);
        super.onTrimMemory(level);
    }

    @NotNull
    @VisibleForTesting //Also used from Public api which is deprecated
    public static SoundCloudApplication fromContext(@NotNull Context c) {
        if (c.getApplicationContext() instanceof SoundCloudApplication) {
            return ((SoundCloudApplication) c.getApplicationContext());
        } else {
            throw new RuntimeException("can't obtain app from context");
        }
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
