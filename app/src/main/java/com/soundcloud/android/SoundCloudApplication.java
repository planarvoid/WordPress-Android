package com.soundcloud.android;

import static com.soundcloud.android.storage.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.storage.provider.ScContentProvider.enableSyncing;

import com.crashlytics.android.Crashlytics;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.localytics.android.Constants;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.LoggingDebugHook;
import com.soundcloud.android.startup.migrations.MigrationEngine;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ExceptionUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Token;
import dagger.ObjectGraph;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.plugins.RxJavaPlugins;

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

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class SoundCloudApplication extends Application {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    // Remove these fields when we've moved to a full DI solution
    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static SoundCloudApplication instance;

    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static ScModelManager sModelManager;

    // needs to remain in memory over the life-time of the app
    @SuppressWarnings("unused")
    private AnalyticsEngine analyticsEngine;

    @Inject
    EventBus eventBus;
    @Inject
    ScModelManager modelManager;
    @Inject
    ImageOperations imageOperations;
    @Inject
    AccountOperations accountOperations;
    @Inject
    ExperimentOperations experimentOperations;
    @Inject
    ApplicationProperties applicationProperties;
    @Inject
    AnalyticsProperties analyticsProperties;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    PlayerWidgetController widgetController;
    @Inject
    DeviceHelper deviceHelper;

    protected ObjectGraph objectGraph;

    public SoundCloudApplication() {
        objectGraph = ObjectGraph.create(
                new ApplicationModule(this),
                new WidgetModule(),
                new SoundCloudModule()
        );
    }

    @VisibleForTesting
    SoundCloudApplication(EventBus eventBus, AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        objectGraph.inject(this);

        // reroute to a static field for legacy code
        sModelManager = modelManager;

        new MigrationEngine(this).migrate();

        Log.i(TAG, "Application starting up in mode " + applicationProperties.getBuildType());
        Log.d(TAG, applicationProperties.toString());

        if (applicationProperties.isDevBuildRunningOnDalvik() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
            RxJavaPlugins.getInstance().registerObservableExecutionHook(new LoggingDebugHook());
        }

        if (ApplicationProperties.shouldReportCrashes() &&
                sharedPreferences.getBoolean(SettingsActivity.CRASH_REPORTING_ENABLED, false)) {
            Crashlytics.start(this);
            ExceptionUtils.setupOOMInterception();
        }

        IOUtils.checkState(this);

        imageOperations.initialise(this, applicationProperties);

        accountOperations.loadLoggedInUser();
        setupCurrentUserAccount();
        setupExperiments();
        setupAnalytics();

        FacebookSSOActivity.extendAccessTokenIfNeeded(this);

        widgetController.subscribe();
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

            try {
                C2DMReceiver.register(this);
            } catch (Exception e){
                SoundCloudApplication.handleSilentException("Could not register c2dm ", e);
            }

            // sync current sets
            AndroidUtils.doOnce(this, "request.sets.sync", new Runnable() {
                @Override
                public void run() {
                    requestSetsSync();
                }
            });

            ContentStats.init(this);

            if (applicationProperties.isBetaBuildRunningOnDalvik()){
                Crashlytics.setUserIdentifier(accountOperations.getLoggedInUser().getUsername());
            }
        }
    }

    private void setupExperiments() {
        experimentOperations.loadAssignment();
    }

    private void setupAnalytics() {
        Log.d(TAG, analyticsProperties.toString());

        // Unfortunately, both Localytics and ComScore are unmockable in tests and were crashing the tests during
        // initialiation of AnalyticsEngine, so we do not register them unless we're running on a real device
        final List<AnalyticsProvider> analyticsProviders;
        if (applicationProperties.isRunningOnDalvik()) {
            analyticsProviders = Lists.newArrayList(
                    new LocalyticsAnalyticsProvider(this, analyticsProperties, accountOperations.getLoggedInUserId()),
                    new EventLoggerAnalyticsProvider(),
                    new ComScoreAnalyticsProvider(this));
        } else {
            analyticsProviders = Collections.emptyList();
        }
        analyticsEngine = new AnalyticsEngine(eventBus, sharedPreferences, analyticsProperties, analyticsProviders);
        Constants.IS_LOGGABLE = analyticsProperties.isAnalyticsAvailable() && applicationProperties.isDebugBuild();
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

    public boolean addUserAccountAndEnableSync(User user, Token token, SignupVia via) {
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
    private void requestSetsSync(){
        Intent intent = new Intent(this, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri);

        startService(intent);
    }

    public static void handleSilentException(@Nullable String message, Throwable e) {
        if (ApplicationProperties.shouldReportCrashes()) {
            Log.e(TAG, "Handling silent exception: " + message, e);
            Crashlytics.setString("message", message);
            Crashlytics.logException(e);
        }
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
                    //.detectDiskReads()
                    //.detectDiskWrites()
                    //.detectNetwork()
                    //.penaltyLog()
                    .detectAll()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    //.detectLeakedSqlLiteObjects()
                    //.penaltyLog()
                    //.penaltyDeath()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }
}