package com.soundcloud.android;

import static android.content.pm.PackageManager.*;
import static com.soundcloud.android.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.provider.ScContentProvider.enableSyncing;

import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.PrefetchHandler;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.Connections;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.cache.UserCache;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.beta.BetaService;
import com.soundcloud.android.service.beta.WifiMonitor;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.ATTracker;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.CloudUtils;
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
import org.codehaus.jackson.map.ObjectMapper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.ContentHandler;
import java.net.ResponseCache;
import java.net.URI;
import java.util.Arrays;

@ReportsCrashes(
        formUri = "https://bugsense.appspot.com/api/acra?api_key=c2486881",
        formKey= "",
        checkReportVersion = true,
        checkReportSender = true)
public class SoundCloudApplication extends Application implements AndroidCloudAPI, CloudAPI.TokenListener, Tracker {

    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static final boolean EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT);
    public static final boolean DALVIK = Build.VERSION.SDK_INT > 0;

    public static final boolean API_PRODUCTION = true;
    public static final TrackCache TRACK_CACHE = new TrackCache();
    public static final UserCache USER_CACHE = new UserCache();

    public static boolean DEV_MODE, BETA_MODE;
    private ImageLoader mImageLoader;

    private ATTracker mTracker;

    private User mLoggedInUser;
    protected Wrapper mCloudApi; /* protected for testing */

    public Comment pendingComment;

    public static boolean useRichNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DEV_MODE = isDevMode();
        BETA_MODE = isBetaMode();
        if (DALVIK) {
            if (!EMULATOR) {
                ACRA.init(this); // don't use ACRA when running unit tests / emulator
                mTracker = new ATTracker(this);
            }
        }
        IOUtils.checkState(this);

        mImageLoader = createImageLoader();
        final Account account = getAccount();
        //noinspection ConstantConditions
        mCloudApi = new Wrapper(
                this,
                getClientId(API_PRODUCTION),
                getClientSecret(API_PRODUCTION),
                REDIRECT_URI,
                account == null ? null : getToken(account),
                API_PRODUCTION ? Env.LIVE : Env.SANDBOX
        );

        mCloudApi.setTokenListener(this);
        mCloudApi.debugRequests = DEV_MODE || !DALVIK;


        if (account != null) {
            FollowStatus.initialize(this, getCurrentUserId());
            Connections.initialize(this, "connections-"+getCurrentUserId());

            if (ContentResolver.getIsSyncable(account, AUTHORITY) < 1) {
                enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
            }

            // remove device url so clients resubmit the registration request with
            // device identifier
            CloudUtils.doOnce(this, "reset.c2dm.reg_id", new Runnable() {
                @Override public void run() {
                    PreferenceManager.getDefaultSharedPreferences(SoundCloudApplication.this)
                            .edit()
                            .remove(C2DMReceiver.PREF_DEVICE_URL)
                            .commit();
                }
            });

            C2DMReceiver.register(this, getLoggedInUser());
        }

        if (BETA_MODE) {
            BetaService.scheduleCheck(this, false);
        }

//        setupStrictMode();

        // make sure the WifiMonitor is disabled when not in beta mode
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, WifiMonitor.class),
                BETA_MODE ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP);
    }

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            final long id = getCurrentUserId();
            if (id != -1) {
                mLoggedInUser = SoundCloudDB.getUserById(getContentResolver(), id);
            }
            // user not in db, fall back to local storage
            if (mLoggedInUser == null) {
                mLoggedInUser = new User();
                mLoggedInUser.id = getAccountDataLong(User.DataKeys.USER_ID);
                mLoggedInUser.username = getAccountData(User.DataKeys.USERNAME);
                mLoggedInUser.permalink = getAccountData(User.DataKeys.USER_PERMALINK);
            }
            mLoggedInUser.via = SignupVia.fromString(getAccountData(User.DataKeys.SIGNUP));
        }
        return mLoggedInUser;
    }


    public void clearSoundCloudAccount(final Runnable success, final Runnable error) {
        mCloudApi.invalidateToken();

        Account account = getAccount();
        if (account != null) {
            getAccountManager().removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        if (future.getResult()) {
                            if (success != null) success.run();
                        } else if (error != null) error.run();
                    } catch (OperationCanceledException e) {
                        if (error != null) error.run();
                    } catch (IOException e) {
                        if (error != null) error.run();
                    } catch (AuthenticatorException e) {
                        if (error != null) error.run();
                    }
                }
            }, /*handler, null == main*/ null);
        }

        FollowStatus.set(null);
        Connections.set(null);
        mLoggedInUser = null;
    }

    private ImageLoader createImageLoader() {
        final File cacheDir = IOUtils.getCacheDir(this);
        ResponseCache cache = FileCache.installFileCache(cacheDir, FileCache.IMAGE_CACHE_AUTO);
        ContentHandler bitmapHandler = new BitmapContentHandler();
        ContentHandler prefetchHandler = new PrefetchHandler();
        if (cache instanceof FileCache) {
            // workaround various SDK bugs by wrapping the handler
            bitmapHandler = FileCache.capture(bitmapHandler, null);
            prefetchHandler = FileCache.capture(prefetchHandler, null);

            ((FileCache)cache).trim(); // ICS has auto trimming
        }
        return new ImageLoader(ImageLoader.DEFAULT_TASK_LIMIT,
                null, /* streamFactory */
                bitmapHandler,
                prefetchHandler,
                ImageLoader.DEFAULT_CACHE_SIZE,
                null  /* handler */);
    }

    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
    }



    public Account getAccount() {
        Account[] account = getAccountManager().getAccountsByType(getString(R.string.account_type));
        if (account.length == 0) {
            return null;
        } else {
            return account[0];
        }
    }

    public AccountManagerFuture<Bundle> addAccount(Activity activity, AccountManagerCallback<Bundle> callback) {
        return getAccountManager().addAccount(
                getString(R.string.account_type),
                Token.ACCESS_TOKEN, null, null, activity, callback, null);
    }

    public boolean addUserAccount(User user, Token token, SignupVia via) {
        final String type = getString(R.string.account_type);
        final Account account = new Account(user.username, type);
        final AccountManager am = getAccountManager();

        final boolean created = am.addAccountExplicitly(account, token.access, null);
        if (created) {
            am.setAuthToken(account, Token.ACCESS_TOKEN,  token.access);
            am.setAuthToken(account, Token.REFRESH_TOKEN, token.refresh);
            am.setUserData(account, Token.SCOPE, token.scope);
            am.setUserData(account, User.DataKeys.USER_ID, Long.toString(user.id));
            am.setUserData(account, User.DataKeys.USERNAME, user.username);
            am.setUserData(account, User.DataKeys.USER_PERMALINK, user.permalink);
            am.setUserData(account, User.DataKeys.SIGNUP, via.name);
        }
        mLoggedInUser = null;
        // move this when we can't guarantee we will only have 1 account active at a time
        FollowStatus.initialize(this, user.id);

        enableSyncing(account, SyncConfig.DEFAULT_SYNC_DELAY);
        return created;
    }

    public Token useAccount(Account account) {
        Token token = getToken(account);
        mCloudApi.setToken(token);
        return token;
    }

    public String getAccountData(String key) {
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

    public long getCurrentUserId()  {
        return getAccountDataLong(User.DataKeys.USER_ID);
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

    private String getClientId(boolean production) {
        return getResources().getString(production ?
                R.string.client_id :
                R.string.sandbox_client_id);
    }

    private String getClientSecret(boolean production) {
        return getResources().getString(production ?
                R.string.client_secret :
                R.string.sandbox_client_secret);
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

    public Token authorizationCode(String code, String... scopes) throws IOException {
        return mCloudApi.authorizationCode(code, scopes);
    }

    public void setDefaultContentType(String contentType) {
        mCloudApi.setDefaultContentType(contentType);
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

    public void onFirstRun(int oldVersionCode, int newVersionCode) {
        for (int i = oldVersionCode; i < newVersionCode; ++i) {
            int nextVersion = i + 1;
            switch (nextVersion) {
                case 31:
                    if (getAccount() != null){ // enable syncing
                        enableSyncing(getAccount(), SyncConfig.DEFAULT_SYNC_DELAY);
                    }
                    break;
                default:
                    break;
            }
        }
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
        return EMULATOR || hasKey(R.array.beta_sigs);
    }

    private boolean isDevMode() {
        return hasKey(R.array.debug_sigs);
    }

    private boolean hasKey(final int resource) {
        try {
             PackageInfo info = getPackageManager().getPackageInfo(
                     getPackageName(),
                     GET_SIGNATURES);
            if (info != null && info.signatures != null) {
                final String[] keys = getResources().getStringArray(resource);
                final String sig =  info.signatures[0].toCharsString();
                Arrays.sort(keys);
                return Arrays.binarySearch(keys, sig) > -1;
            } else {
                return false;
            }
        } catch (NameNotFoundException ignored) {
            return false;
        }
    }

    /**
     * @param msg    message
     * @param e      exception, can be null
     * @return       the thread used to submit the msg
     */
    public static Thread handleSilentException(String msg, Exception e) {
        if (EMULATOR || !DALVIK) return null; // acra is disabled on emulator
        if (msg != null) {
           Log.w(TAG, "silentException: "+msg, e);
           ACRA.getErrorReporter().putCustomData("message", msg);
        }
        return ACRA.getErrorReporter().handleSilentException(e);
    }

    public static SoundCloudApplication fromContext(Context c){
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

    private static void setupStrictMode() {
        if (Build.VERSION.SDK_INT > 8) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }
}