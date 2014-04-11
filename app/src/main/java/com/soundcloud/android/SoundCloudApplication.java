package com.soundcloud.android;

import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys;
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
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.startup.migrations.MigrationEngine;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Token;
import dagger.ObjectGraph;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static SoundCloudApplication instance;

    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static ScModelManager sModelManager;

    private User mLoggedInUser;

    // needs to remain in memory over the life-time of the app
    @SuppressWarnings("unused")
    private AnalyticsEngine mAnalyticsEngine;

    @Inject
    EventBus mEventBus;
    @Inject
    ScModelManager mModelManager;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    AccountOperations mAccountOperations;
    @Inject
    ApplicationProperties mApplicationProperties;
    @Inject
    AnalyticsProperties mAnalyticsProperties;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    PlayerWidgetController mWidgetController;

    protected ObjectGraph mObjectGraph;

    // DO NOT REMOVE, Android needs a default constructor.
    public SoundCloudApplication() {
        mObjectGraph = ObjectGraph.create(new ApplicationModule(this), new WidgetModule(), new SoundCloudModule());
    }

    @VisibleForTesting
    SoundCloudApplication(EventBus eventBus, AccountOperations accountOperations) {
        mEventBus = eventBus;
        mAccountOperations = accountOperations;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        mObjectGraph.inject(this);

        // reroute to a static field for legacy code
        sModelManager = mModelManager;

        new MigrationEngine(this).migrate();

        Log.i(TAG, "Application starting up in mode " + mApplicationProperties.getBuildType());
        Log.d(TAG, mApplicationProperties.toString());

        if (mApplicationProperties.isDevBuildRunningOnDalvik() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
        }

        if (ApplicationProperties.shouldReportCrashes() &&
                mSharedPreferences.getBoolean(SettingsActivity.CRASH_REPORTING_ENABLED, false)) {
            Crashlytics.start(this);
            setupOOMInterception();
        }

        IOUtils.checkState(this);

        mImageOperations.initialise(this, mApplicationProperties);

        setupCurrentUserAccount();

        setupAnalytics();

        FacebookSSOActivity.extendAccessTokenIfNeeded(this);

        mWidgetController.subscribe();
    }

    private void setupCurrentUserAccount() {
        final Account account = mAccountOperations.getSoundCloudAccount();

        if (account != null) {
            if (ContentResolver.getIsSyncable(account, AUTHORITY) < 1) {
                enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            }

            // remove device url so clients resubmit the registration request with
            // device identifier
            AndroidUtils.doOnce(this, "reset.c2dm.reg_id", new Runnable() {
                @Override
                public void run() {
                    mSharedPreferences.edit().remove(Consts.PrefKeys.C2DM_DEVICE_URL).apply();
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

            if (mApplicationProperties.isBetaBuildRunningOnDalvik()){
                Crashlytics.setUserIdentifier(getLoggedInUser().username);
            }
        }
    }

    /*
     * This must be called AFTER Crashlytics has been initialised
     */
    private void setupOOMInterception() {
        final Thread.UncaughtExceptionHandler crashlyticsHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                if (e instanceof OutOfMemoryError) {
                    crashlyticsHandler.uncaughtException(thread, new OutOfMemoryError("OOM Trend"));
                } else {
                    crashlyticsHandler.uncaughtException(thread, e);
                }
            }
        });
    }

    private void setupAnalytics() {
        Log.d(TAG, mAnalyticsProperties.toString());

        // Unfortunately, both Localytics and ComScore are unmockable in tests and were crashing the tests during
        // initialiation of AnalyticsEngine, so we do not register them unless we're running on a real device
        final List<AnalyticsProvider> analyticsProviders;
        if (mApplicationProperties.isRunningOnDalvik()) {
            analyticsProviders = Lists.newArrayList(
                    new LocalyticsAnalyticsProvider(this, mAnalyticsProperties, getCurrentUserId()),
                    new EventLoggerAnalyticsProvider(),
                    new ComScoreAnalyticsProvider(this));
        } else {
            analyticsProviders = Collections.emptyList();
        }
        mAnalyticsEngine = new AnalyticsEngine(mEventBus, mSharedPreferences, mAnalyticsProperties, analyticsProviders);
        Constants.IS_LOGGABLE = mAnalyticsProperties.isAnalyticsAvailable() && mApplicationProperties.isDebugBuild();
    }

    public EventBus getEventBus() {
        return mEventBus;
    }

    @Deprecated // use @Inject instead!
    public ImageOperations getImageOperations() {
        return mImageOperations;
    }

    @NotNull
    public static ObjectGraph getObjectGraph() {
        if (instance == null || instance.mObjectGraph == null) {
            throw new IllegalStateException(
                    "Cannot access the app graph before the application has been created");
        }
        return instance.mObjectGraph;
    }

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            final long id = mAccountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
            if (id != -1) {
                mLoggedInUser = sModelManager.getUser(id);
            }
            // user not in db, fall back to local storage
            if (mLoggedInUser == null) {
                User user = new User();
                user.setId(mAccountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey()));
                user.username = mAccountOperations.getAccountDataString(AccountInfoKeys.USERNAME.getKey());
                user.permalink = mAccountOperations.getAccountDataString(AccountInfoKeys.USER_PERMALINK.getKey());
                return user;
            }
            mLoggedInUser.via = SignupVia.fromString(mAccountOperations.getAccountDataString(AccountInfoKeys.SIGNUP.getKey()));
        }
        return mLoggedInUser;
    }

    public void clearLoggedInUser() {
        mLoggedInUser = null;
    }

    //TODO Move this into AccountOperations once we refactor User info out of here
    public boolean addUserAccountAndEnableSync(User user, Token token, SignupVia via) {
        Account account = mAccountOperations.addOrReplaceSoundCloudAccount(user, token, via);
        if (account != null) {
            mLoggedInUser = user;

            mEventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(user));

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

    private long getCurrentUserId()  {
        return mLoggedInUser == null ? mAccountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey()) : mLoggedInUser.getId();
    }

    public static long getUserId() {
        return instance.getCurrentUserId();
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

    public static long getUserIdFromContext(Context c){
        SoundCloudApplication app = fromContext(c);
        return app == null ? -1 : app.getCurrentUserId();
    }

    // keep this until we've sorted out RL2, since some tests rely on the getUserId stuff which in turn requires
    // a valid AccountOps instance
    public void setAccountOperations(AccountOperations operations) {
        mAccountOperations = operations;
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