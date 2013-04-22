package com.soundcloud.android;

import static com.soundcloud.android.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.provider.ScContentProvider.enableSyncing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.imageloader.DownloadBitmapHandler;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.imageloader.PrefetchHandler;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
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
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Stream;
import com.soundcloud.api.Token;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@ReportsCrashes(
        formUri = "https://bugsense.appspot.com/api/acra?api_key=3e22e330",
        formKey= "",
        checkReportVersion = true,
        checkReportSender = true)
public class SoundCloudApplication extends Application implements AndroidCloudAPI, CloudAPI.TokenListener, Tracker {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static final boolean EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
                                           "full_x86".equals(Build.PRODUCT)   || "sdk_x86".equals(Build.PRODUCT);

    public static final boolean DALVIK = Build.PRODUCT != null;
    public static boolean DEV_MODE, BETA_MODE;
    private ImageLoader mImageLoader;

    @Deprecated public static ScModelManager MODEL_MANAGER;

    private ATTracker mTracker;

    private User mLoggedInUser;
    protected Wrapper mCloudApi; /* protected for testing */

    public static SoundCloudApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        DEV_MODE = isDevMode();
        BETA_MODE = isBetaMode();

        if (DEV_MODE && !ActivityManager.isUserAMonkey()) {
            setupStrictMode();
        }

        if (DALVIK && !EMULATOR) {
            ACRA.init(this); // don't use ACRA when running unit tests / emulator
            mTracker = new ATTracker(this);
        }
        instance = this;
        IOUtils.checkState(this);
        mImageLoader = createImageLoader();
        final Account account = getAccount();

        mCloudApi = Wrapper.create(this, account == null ? null : getToken(account));
        mCloudApi.setTokenListener(this);

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
                C2DMReceiver.register(this, getCurrentUserId());
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

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            final long id = getAccountDataLong(User.DataKeys.USER_ID);
            if (id != -1) {
                mLoggedInUser = MODEL_MANAGER.getUser(id);
            }
            // user not in db, fall back to local storage
            if (mLoggedInUser == null) {
                User user = new User();
                user.id = getAccountDataLong(User.DataKeys.USER_ID);
                user.username = getAccountData(User.DataKeys.USERNAME);
                user.permalink = getAccountData(User.DataKeys.USER_PERMALINK);
                return user;
            }
            mLoggedInUser.via = SignupVia.fromString(getAccountData(User.DataKeys.SIGNUP));
        }
        return mLoggedInUser;
    }

    public void clearSoundCloudAccount(@Nullable final Runnable onSuccess, @Nullable final Runnable onError) {
        final Account account = getAccount();
        if (account != null) {
            getAccountManager().removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        if (future.getResult()) {
                            onAccountRemoved(account);
                            if (onSuccess != null) onSuccess.run();
                        } else if (onError != null) onError.run();
                    } catch (OperationCanceledException e) {
                        if (onError != null) onError.run();
                    } catch (IOException e) {
                        if (onError != null) onError.run();
                    } catch (AuthenticatorException e) {
                        if (onError != null) onError.run();
                    }
                }
            }, /*handler, null == main*/ null);
        } else if (onError != null) {
            onError.run();
        }
    }

    //TODO: This should be on a different class, e.g. a LoginManager
    public void onAccountRemoved(Account account) {
        new Thread() {
            @Override
            public void run() {
                new UserStorage(SoundCloudApplication.this).clearLoggedInUser();
                new ActivitiesStorage(SoundCloudApplication.this).clear(null);

                PlayQueueManager.clearState(SoundCloudApplication.this);
                FacebookSSO.FBToken.clear(SoundCloudApplication.instance);
            }
        }.run();

        sendBroadcast(new Intent(Actions.LOGGING_OUT));
        sendBroadcast(new Intent(CloudPlaybackService.RESET_ALL));

        C2DMReceiver.unregister(this);
        FollowStatus.set(null);
        ConnectionsCache.set(null);
        mLoggedInUser = null;
        mCloudApi.invalidateToken();
    }

    protected ImageLoader createImageLoader() {
        FileCache.installFileCache(IOUtils.getCacheDir(this));
        return new ImageLoader(new DownloadBitmapHandler(),
                new PrefetchHandler(),
                ImageLoader.DEFAULT_CACHE_SIZE, ImageLoader.DEFAULT_TASK_LIMIT);
    }

    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
    }

    public @Nullable Account getAccount() {
        Account[] account = getAccountManager().getAccountsByType(getString(R.string.account_type));
        if (account.length == 0) {
            return null;
        } else {
            return account[0];
        }
    }

    public AccountManagerFuture<Bundle> addAccount(Activity activity) {
        return getAccountManager().addAccount(
                getString(R.string.account_type),
                Token.ACCESS_TOKEN, null, null, activity, null, null);
    }

    /**
     * @see #addAccount(android.content.Context, com.soundcloud.android.model.User, com.soundcloud.api.Token,
     * com.soundcloud.android.activity.auth.SignupVia)
     */
    public boolean addUserAccountAndEnableSync(User user, Token token, SignupVia via) {
        Account account = addAccount(this, user, token, via);
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

    /**
     * Adds the given user as a SoundCloud account to the device's list of accounts. Idempotent, will be a no op if
     * already called.
     *
     * @return the new account, or null if account already existed or adding it failed
     */
    public static Account addAccount(Context context, User user, Token token, SignupVia via) {
        final String type = context.getString(R.string.account_type);
        final Account account = new Account(user.username, type);
        final AccountManager am = AccountManager.get(context);
        final boolean created = am.addAccountExplicitly(account, token.access, null);
        if (created) {
            am.setAuthToken(account, Token.ACCESS_TOKEN,  token.access);
            am.setAuthToken(account, Token.REFRESH_TOKEN, token.refresh);
            am.setUserData(account, Token.SCOPE, token.scope);
            am.setUserData(account, User.DataKeys.USER_ID, Long.toString(user.id));
            am.setUserData(account, User.DataKeys.USERNAME, user.username);
            am.setUserData(account, User.DataKeys.USER_PERMALINK, user.permalink);
            am.setUserData(account, User.DataKeys.SIGNUP, via.name);
            return account;
        } else {
            return null;
        }
    }

    public Token useAccount(Account account) {
        Token token = getToken(account);
        mCloudApi.setToken(token);
        return token;
    }

    public @Nullable String getAccountData(String key) {
        Account account = getAccount();
        return account == null ? null : getAccountManager().getUserData(account, key);
    }

    public int getAccountDataInt(String key) {
        String data = getAccountData(key);
        return data == null ? -1 : Integer.parseInt(data);
    }

    public long getAccountDataLong(String key) {
        String data = getAccountData(key);
        return data == null ? -1 : Long.parseLong(data);
    }

    public boolean getAccountDataBoolean(String key) {
        String data = getAccountData(key);
        return data != null && Boolean.parseBoolean(data);
    }

    private long getCurrentUserId()  {
        return mLoggedInUser == null ? getAccountDataLong(User.DataKeys.USER_ID) : mLoggedInUser.id;
    }

    public static long getUserId() {
        return instance.getCurrentUserId();
    }

    public boolean setAccountData(String key, boolean value) {
        return setAccountData(key, Boolean.toString(value));
    }

    public boolean setAccountData(String key, long value) {
        return setAccountData(key, Long.toString(value));
    }

    public boolean setAccountData(String key, String value) {
        Account account = getAccount();
        if (account == null) {
            return false;
        } else {
            /*
            TODO: not sure : setUserData off the ui thread??
                StrictMode policy violation; ~duration=161 ms: android.os.StrictMode$StrictModeDiskWriteViolation: policy=279 violation=1

                D/StrictMode(15333): 	at android.os.StrictMode.readAndHandleBinderCallViolations(StrictMode.java:1617)
                D/StrictMode(15333): 	at android.os.Parcel.readExceptionCode(Parcel.java:1309)
                D/StrictMode(15333): 	at android.os.Parcel.readException(Parcel.java:1278)
                D/StrictMode(15333): 	at android.accounts.IAccountManager$Stub$Proxy.setUserData(IAccountManager.java:701)
                D/StrictMode(15333): 	at android.accounts.AccountManager.setUserData(AccountManager.java:684)
                D/StrictMode(15333): 	at com.soundcloud.android.SoundCloudApplication.setAccountData(SoundCloudApplication.java:314)
             */
            getAccountManager().setUserData(account, key, value);
            return true;
        }
    }

    private Token getToken(Account account) {
        return new Token(getAccessToken(account), getRefreshToken(account), getAccountData(Token.SCOPE));
    }

    private String getAccessToken(Account account) {
        return getAccountManager().peekAuthToken(account, Token.ACCESS_TOKEN);
    }

    private String getRefreshToken(Account account) {
        return getAccountManager().peekAuthToken(account, Token.REFRESH_TOKEN);
    }

    private AccountManager getAccountManager() {
        return AccountManager.get(this);
    }

    public HttpResponse head(Request resource) throws IOException {
        return mCloudApi.head(resource);
    }

    public HttpResponse get(Request resource) throws IOException {
        return mCloudApi.get(resource);
    }

    public Token clientCredentials(String... scopes) throws IOException {
        return mCloudApi.clientCredentials(scopes);
    }

    public Token extensionGrantType(String grantType, String... scopes) throws IOException {
        return mCloudApi.extensionGrantType(grantType, scopes);
    }

    public Token login(String username, String password, String... scopes) throws IOException {
        return mCloudApi.login(username, password, scopes);
    }

    public URI authorizationCodeUrl(String... options) {
        return mCloudApi.authorizationCodeUrl(options);
    }

    public HttpResponse put(Request request) throws IOException {
        return mCloudApi.put(request);
    }

    public HttpResponse post(Request request) throws IOException {
        return mCloudApi.post(request);
    }

    public HttpResponse delete(Request request) throws IOException {
        return mCloudApi.delete(request);
    }

    public Token refreshToken() throws IOException {
        return mCloudApi.refreshToken();
    }

    public Token getToken() {
        return mCloudApi.getToken();
    }

    public long resolve(String uri) throws IOException {
        return mCloudApi.resolve(uri);
    }

    public void setToken(Token token) {
        mCloudApi.setToken(token);
    }

    public void setTokenListener(TokenListener listener) {
        mCloudApi.setTokenListener(listener);
    }

    public Token exchangeOAuth1Token(String oauth1AccessToken) throws IOException {
        return mCloudApi.exchangeOAuth1Token(oauth1AccessToken);
    }

    public Token invalidateToken() {
        return mCloudApi.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
    }

    public Context getContext() {
        return this;
    }

    public <T extends ScResource> T read(Request req) throws IOException {
        return mCloudApi.read(req);
    }

    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        return mCloudApi.update(request);
    }

    public <T extends ScResource> T create(Request request) throws IOException {
        return mCloudApi.create(request);
    }

    public <T extends ScResource> List<T> readList(Request req) throws IOException {
        return mCloudApi.readList(req);
    }

    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        return mCloudApi.readCollection(req);
    }

    @NotNull public <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request, Class<C> ch) throws IOException {
        return mCloudApi.readFullCollection(request, ch);
    }


    public <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        return mCloudApi.readListFromIds(request, ids);
    }

    public Token authorizationCode(String code, String... scopes) throws IOException {
        return mCloudApi.authorizationCode(code, scopes);
    }

    public void setDefaultContentType(String contentType) {
        mCloudApi.setDefaultContentType(contentType);
    }

    public void setDefaultAcceptEncoding(String encoding) {
        mCloudApi.setDefaultAcceptEncoding(encoding);
    }

    public HttpClient getHttpClient() {
        return mCloudApi.getHttpClient();
    }

    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        return mCloudApi.safeExecute(target, request);
    }

    public Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException {
        return mCloudApi.resolveStreamUrl(uri, skipLogging);
    }

    @Override
    public String getUserAgent() {
        return mCloudApi.getUserAgent();
    }

    @Override
    public Env getEnv() {
        return mCloudApi.getEnv();
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

    @Override
    public Token onTokenInvalid(final Token expired) {
        try {
            final Account acc = getAccount();
            if (acc != null) {
               Token newToken = getToken(acc);
                if (!newToken.equals(expired)) {
                    return newToken;
                }
            }
            return null;
        } finally {
            getAccountManager().invalidateAuthToken(
                getString(R.string.account_type),
                expired.access);

            getAccountManager().invalidateAuthToken(
                getString(R.string.account_type),
                expired.refresh);
        }
    }

    @Override
    public void onTokenRefreshed(Token token) {
        Account account = getAccount();
        AccountManager am = getAccountManager();
        if (account != null && token.valid() && token.defaultScoped()) {
            am.setPassword(account, token.access);
            am.setAuthToken(account, Token.ACCESS_TOKEN, token.access);
            am.setAuthToken(account, Token.REFRESH_TOKEN, token.refresh);
            am.setUserData(account, Token.EXPIRES_IN, "" + token.expiresIn);
            am.setUserData(account, Token.SCOPE, token.scope);
        }
    }

    private boolean isBetaMode() {
        return AndroidUtils.appSignedBy(this, getResources().getStringArray(R.array.beta_sigs));
    }

    private boolean isDevMode() {
        return AndroidUtils.appSignedBy(this, getResources().getStringArray(R.array.debug_sigs));
    }

    /**
     * @param msg    message
     * @param e      exception, can be null
     * @return       the thread used to submit the msg
     */
    public static Thread handleSilentException(@Nullable String msg, Exception e) {
        if (EMULATOR || !DALVIK) return null; // acra is disabled on emulator
        if (msg != null) {
           Log.w(TAG, "silentException: "+msg, e);
           ACRA.getErrorReporter().putCustomData("message", msg);
        }
        return ACRA.getErrorReporter().handleSilentException(new SilentException(e));
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

    @Override @TargetApi(14)
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        /*if (level >= TRIM_MEMORY_RUNNING_CRITICAL) ImageLoader.get(this).onLowMemory(); */
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