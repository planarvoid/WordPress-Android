package com.soundcloud.android;

import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys;
import static com.soundcloud.android.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.provider.ScContentProvider.enableSyncing;

import com.crashlytics.android.Crashlytics;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.migrations.MigrationEngine;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.User;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.android.tracking.ATTracker;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;

public class SoundCloudApplication extends Application implements Tracker {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    @Deprecated public static ScModelManager MODEL_MANAGER;

    private ATTracker mTracker;

    private User mLoggedInUser;
    private AccountOperations accountOperations;

    public static Context instance;

    @Override
    public void onCreate() {
        super.onCreate();

        new MigrationEngine(this).migrate();

        ApplicationProperties appProperties = new ApplicationProperties(getResources());
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(getResources());

        Log.i(TAG, "Application starting up in mode " + appProperties.getBuildType());
        Log.d(TAG, appProperties.toString());
        Log.d(TAG, analyticsProperties.toString());

        if (appProperties.isDevBuildRunningOnDalvik() && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
        }

        if(analyticsProperties.isAnalyticsEnabled()){
            mTracker = new ATTracker(this);
        }

        if (ApplicationProperties.shouldReportCrashes()) {
            Crashlytics.start(this);
        }
        instance = this;
        IOUtils.checkState(this);
        createImageLoader();

        accountOperations = new AccountOperations(this);
        final Account account = accountOperations.getSoundCloudAccount();


        MODEL_MANAGER = new ScModelManager(this);

        if (account != null) {
            if (ContentResolver.getIsSyncable(account, AUTHORITY) < 1) {
                enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            }

            // remove device url so clients resubmit the registration request with
            // device identifier
            AndroidUtils.doOnce(this, "reset.c2dm.reg_id", new Runnable() {
                @Override
                public void run() {
                    PreferenceManager.getDefaultSharedPreferences(SoundCloudApplication.this)
                            .edit()
                            .remove(Consts.PrefKeys.C2DM_DEVICE_URL)
                            .commit();
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
        }

        FacebookSSO.extendAccessTokenIfNeeded(this);
    }

    public Long getLoggedInUsersId(){
        return getLoggedInUser().getId();
    }

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            final long id = accountOperations.getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
            if (id != -1) {
                mLoggedInUser = MODEL_MANAGER.getUser(id);
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

    protected void createImageLoader() {
        ImageLoader.getInstance().init(
                new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(ImageOptionsFactory.cache())
                .build()
        );

        FileCache.installFileCache(IOUtils.getCacheDir(this));
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
        return ((SoundCloudApplication) instance).getCurrentUserId();
    }

    public void track(Event event, Object... args) {
        if (mTracker != null) mTracker.track(event, args);
    }

    public void track(Class<?> klazz, Object... args) {
        Tracking tracking = klazz.getAnnotation(Tracking.class);
        if (mTracker != null && tracking != null) {
            if (tracking.page() != Page.UNKNOWN) track(tracking.page(), args);
            if (tracking.click() != Click.UNKNOWN) track(tracking.click(), args);
        }
    }

    public static void handleSilentException(@Nullable String message, Throwable e) {
        if (ApplicationProperties.shouldReportCrashes()) {
            Log.e(TAG, "Handling silent exception L " + message, e);
            Crashlytics.logException(e);
        }
    }

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

    private static class SilentException extends Exception {
        private SilentException(Throwable throwable) {
            super(throwable);
        }
    }

}