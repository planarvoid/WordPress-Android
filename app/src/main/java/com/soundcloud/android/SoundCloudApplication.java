package com.soundcloud.android;

import static com.soundcloud.android.storage.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.storage.provider.ScContentProvider.enableSyncing;

import com.crashlytics.android.Crashlytics;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.cast.CastSessionReconnector;
import com.soundcloud.android.configuration.ConfigurationFeatureController;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.events.DeviceMetricsEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.LegacyModule;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.peripherals.PeripheralsController;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.playback.service.PlaybackServiceModule;
import com.soundcloud.android.playback.service.skippy.SkippyFactory;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.playback.widget.WidgetModule;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.settings.GeneralSettings;
import com.soundcloud.android.startup.migrations.MigrationEngine;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.sync.SyncModule;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.CrashlyticsMemoryReporter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.MemoryReporter;
import dagger.ObjectGraph;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import javax.inject.Inject;

public class SoundCloudApplication extends Application {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    // Remove these fields when we've moved to a full DI solution
    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static SoundCloudApplication instance;

    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static ScModelManager sModelManager;

    // These are not injected because we need them before Dagger initializes
    private MemoryReporter memoryReporter;
    private SharedPreferences sharedPreferences;
    private ApplicationProperties applicationProperties;

    @Inject MigrationEngine migrationEngine;
    @Inject EventBus eventBus;
    @Inject ScModelManager modelManager;
    @Inject ImageOperations imageOperations;
    @Inject AccountOperations accountOperations;
    @Inject ConfigurationOperations configOperations;
    @Inject PlayerWidgetController widgetController;
    @Inject PeripheralsController peripheralsController;
    @Inject PlaySessionController playSessionController;
    @Inject PlaySessionStateProvider playSessionStateProvider;
    @Inject AdsController adsController;
    @Inject PlaylistTagStorage playlistTagStorage;
    @Inject PlaybackNotificationController playbackNotificationController;
    @Inject SkippyFactory skippyFactory;
    @Inject FeatureFlags featureFlags;
    @Inject CryptoOperations cryptoOperations;
    @Inject ConfigurationFeatureController configurationFeatureController;
    @Inject CastSessionReconnector castSessionReconnector;

    // we need this object to exist throughout the life time of the app,
    // even if it appears to be unused
    @Inject @SuppressWarnings("unused") AnalyticsEngine analyticsEngine;

    protected ObjectGraph objectGraph;

    public SoundCloudApplication() {
        objectGraph = ObjectGraph.create(
                new ApplicationModule(this),
                new AnalyticsModule(),
                new WidgetModule(),
                new LegacyModule(),
                new FeaturesModule(),
                new SyncModule(),
                new PlaybackServiceModule());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initializePreInjectionObjects();
        setUpCrashReportingIfNeeded();
        objectGraph.inject(this);

        // reroute to a static field for legacy code
        sModelManager = modelManager;

        migrationEngine.migrate();

        Log.i(TAG, "Application starting up in mode " + applicationProperties.getBuildType());
        Log.d(TAG, applicationProperties.toString());

        if (applicationProperties.isDevBuildRunningOnDevice() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
        }


        memoryReporter.reportSystemMemoryStats();

        IOUtils.createCacheDirectories(this);

        // initialise skippy so it can do it's expensive one-shot ops
        skippyFactory.create().preload(this);

        imageOperations.initialise(this, applicationProperties);

        accountOperations.loadLoggedInUser();
        configOperations.update();
        setupCurrentUserAccount();
        generateDeviceKey();

        FacebookSSOActivity.extendAccessTokenIfNeeded(this);

        playlistTagStorage.resetPopularTags();

        widgetController.subscribe();
        peripheralsController.subscribe();
        playSessionController.subscribe();
        playSessionStateProvider.subscribe();
        playbackNotificationController.subscribe();
        adsController.subscribe();

        if (featureFlags.isEnabled(Flag.GOOGLE_CAST)) {
            castSessionReconnector.startListening();
        }

        configurationFeatureController.subscribe();

        publishDeviceMetrics();
    }

    private void publishDeviceMetrics() {
        final long dbSize = DatabaseManager.getDatabaseFileSize(this);
        eventBus.publish(EventQueue.TRACKING, DeviceMetricsEvent.forDatabaseSize(dbSize));
    }

    private void generateDeviceKey() {
        cryptoOperations.generateAndStoreDeviceKeyIfNeeded();
    }

    private void initializePreInjectionObjects() {
        applicationProperties = new ApplicationProperties(getResources());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (isReportingCrashes()) {
            memoryReporter = new CrashlyticsMemoryReporter(activityManager);
        } else {
            memoryReporter = new MemoryReporter(activityManager);
        }
    }

    private void setUpCrashReportingIfNeeded() {
        if (isReportingCrashes()) {
            Crashlytics.start(this);
            ErrorUtils.setupUncaughtExceptionHandler(memoryReporter);
        }
    }

    private void setupCurrentUserAccount() {
        final Account account = accountOperations.getSoundCloudAccount();

        if (account != null) {
            if (ContentResolver.getIsSyncable(account, AUTHORITY) < 1) {
                enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            }

            // remove device url so clients resubmit the registration request with
            // device identifier
            AndroidUtils.doOnce(this, "reset.c2dm.reg_id", new Runnable() {
                @Override
                public void run() {
                    sharedPreferences.edit().remove(Consts.PrefKeys.C2DM_DEVICE_URL).apply();
                }
            });
            // delete old cache dir
            AndroidUtils.doOnce(this, "delete.old.cache.dir", new Runnable() {
                @Override public void run() {
                    IOUtils.deleteDir(Consts.OLD_EXTERNAL_CACHE_DIRECTORY);
                }
            });

            // sync current sets
            AndroidUtils.doOnce(this, "request.sets.sync", new Runnable() {
                @Override
                public void run() {
                    requestSetsSync();
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
    public static ObjectGraph getObjectGraph() {
        if (instance == null || instance.objectGraph == null) {
            throw new IllegalStateException(
                    "Cannot access the app graph before the application has been created");
        }
        return instance.objectGraph;
    }

    public boolean addUserAccountAndEnableSync(PublicApiUser user, Token token, SignupVia via) {
        Account account = accountOperations.addOrReplaceSoundCloudAccount(user, token, via);
        if (account != null) {
            // move this when we can't guarantee we will only have 1 account active at a time
            enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);

            // sync shortcuts so suggest works properly
            Intent intent = new Intent(this, ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(Content.ME_SHORTCUT.uri);

            startService(intent);

            requestSetsSync();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Make sure that sets are synced first, to avoid running into data consistency issues around adding tracks
     * to playlists, see https://github.com/soundcloud/SoundCloud-Android/issues/609
     *
     * Alternatively, sync sets lazily where needed.
     */
    private void requestSetsSync() {
        Intent intent = new Intent(this, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri);

        startService(intent);
    }

    @NotNull
    public static SoundCloudApplication fromContext(@NotNull Context c){
        if (c.getApplicationContext() instanceof  SoundCloudApplication) {
            return ((SoundCloudApplication) c.getApplicationContext());
        } else {
            throw new RuntimeException("can't obtain app from context");
        }
    }

    // keep this until we've sorted out RL2, since some tests rely on the getUserId stuff which in turn requires
    // a valid AccountOps instance
    public void setAccountOperations(AccountOperations operations) {
        accountOperations = operations;
    }

    @TargetApi(9)
    private static void setupStrictMode() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    @Override @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void onLowMemory() {
        onTrimMemory(TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onTrimMemory(int level) {
        memoryReporter.reportMemoryTrim(level);
    }

    private boolean isReportingCrashes() {
        return applicationProperties.shouldReportCrashes() &&
                sharedPreferences.getBoolean(GeneralSettings.CRASH_REPORTING_ENABLED, true);
    }

}
