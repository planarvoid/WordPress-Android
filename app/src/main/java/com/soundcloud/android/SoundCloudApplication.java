package com.soundcloud.android;

import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys;
import static com.soundcloud.android.storage.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.storage.provider.ScContentProvider.enableSyncing;

import com.crashlytics.android.Crashlytics;
import com.localytics.android.Constants;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.dagger.ObjectGraphProvider;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.migrations.MigrationEngine;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.FacebookSSOActivity;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.properties.ApplicationProperties;
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
import android.preference.PreferenceManager;

public class SoundCloudApplication extends Application implements ObjectGraphProvider {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    // Remove these fields when we've moved to a full DI solution
    @Deprecated
    public static SoundCloudApplication instance;

    @Deprecated
    @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"})
    public static ScModelManager sModelManager;

    private User mLoggedInUser;
    private AccountOperations accountOperations;
    private AnalyticsEngine mAnalyticsEngine;

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        sModelManager = new ScModelManager(this);
        instance = this;

        mObjectGraph = ObjectGraph.create(new ApplicationModule(this));

        new MigrationEngine(this).migrate();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ApplicationProperties appProperties = new ApplicationProperties(getResources());
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(getResources(), sharedPreferences);

        Log.i(TAG, "Application starting up in mode " + appProperties.getBuildType());
        Log.d(TAG, appProperties.toString());
        Log.d(TAG, analyticsProperties.toString());

        if (appProperties.isDevBuildRunningOnDalvik() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
        }

        if (analyticsProperties.isAnalyticsEnabled()){
            Constants.IS_LOGGABLE = appProperties.isDebugBuild();
            mAnalyticsEngine = new AnalyticsEngine(this, analyticsProperties);
        }

        if (ApplicationProperties.shouldReportCrashes()) {
            Crashlytics.start(this);
        }

        IOUtils.checkState(this);

        ImageOperations imageOperations = ImageOperations.newInstance();
        imageOperations.initialise(this);

        accountOperations = new AccountOperations(this);
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
                    sharedPreferences.edit().remove(Consts.PrefKeys.C2DM_DEVICE_URL).commit();
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
                SoundCloudApplication.handleSilentException("Could not register c2dm ",e);
            }

            // sync current sets
            AndroidUtils.doOnce(this, "request.sets.sync", new Runnable() {
                @Override
                public void run() {
                    requestSetsSync();
                }
            });

            ContentStats.init(this);

            if (appProperties.isBetaBuildRunningOnDalvik()){
                Crashlytics.setUserIdentifier(getLoggedInUser().username);
            }
        }

        FacebookSSOActivity.extendAccessTokenIfNeeded(this);
    }

    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            final long id = accountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
            if (id != -1) {
                mLoggedInUser = sModelManager.getUser(id);
            }
            // user not in db, fall back to local storage
            if (mLoggedInUser == null) {
                User user = new User();
                user.setId(accountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey()));
                user.username = accountOperations.getAccountDataString(AccountInfoKeys.USERNAME.getKey());
                user.permalink = accountOperations.getAccountDataString(AccountInfoKeys.USER_PERMALINK.getKey());
                return user;
            }
            mLoggedInUser.via = SignupVia.fromString(accountOperations.getAccountDataString(AccountInfoKeys.SIGNUP.getKey()));
        }
        return mLoggedInUser;
    }

    public void clearLoggedInUser() {
        mLoggedInUser = null;
    }

    //TODO Move this into AccountOperations once we refactor User info out of here
    public boolean addUserAccountAndEnableSync(User user, Token token, SignupVia via) {
        Account account = accountOperations.addOrReplaceSoundCloudAccount(user, token, via);
        if (account != null) {
            mLoggedInUser = user;

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

    public AnalyticsEngine getAnalyticsEngine() {
        return mAnalyticsEngine;
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
        return mLoggedInUser == null ? accountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey()) : mLoggedInUser.getId();
    }

    public static long getUserId() {
        return instance.getCurrentUserId();
    }

    public static void handleSilentException(@Nullable String message, Throwable e) {
        if (ApplicationProperties.shouldReportCrashes()) {
            Log.e(TAG, "Handling silent exception L " + message, e);
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

    @TargetApi(9)
    private static void setupStrictMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
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