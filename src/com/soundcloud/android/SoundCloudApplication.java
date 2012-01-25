package com.soundcloud.android;

import android.accounts.*;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.*;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.beta.BetaService;
import com.soundcloud.android.service.beta.WifiMonitor;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.*;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.ContentHandler;
import java.net.ResponseCache;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.content.pm.PackageManager.*;
import static com.soundcloud.android.provider.ScContentProvider.AUTHORITY;
import static com.soundcloud.android.provider.ScContentProvider.enableSyncing;


@ReportsCrashes(
        formUri = "https://bugsense.appspot.com/api/acra?api_key=e937d649",
        formKey= "",
        checkReportVersion = true,
        checkReportSender = true)
public class SoundCloudApplication extends Application implements AndroidCloudAPI, CloudAPI.TokenListener {

    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static final boolean EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT);
    public static final boolean DALVIK = Build.VERSION.SDK_INT > 0;

    public static final boolean API_PRODUCTION = true;
    public static final TrackCache TRACK_CACHE = new TrackCache();
    public static final UserCache USER_CACHE = new UserCache();

    public static boolean DEV_MODE, BETA_MODE;
    private RecordListener mRecListener;
    private ImageLoader mImageLoader;

    private GoogleAnalyticsTracker mTracker;

    private User mLoggedInUser;
    protected Wrapper mCloudApi; /* protected for testing */

    public Comment pendingComment;

    @Override
    public void onCreate() {
        super.onCreate();
        DEV_MODE = isDevMode();
        BETA_MODE = isBetaMode();

        if (DALVIK) {
            if (!EMULATOR) {
                ACRA.init(this); // don't use ACRA when running unit tests / emulator

                mTracker = GoogleAnalyticsTracker.getInstance();
                mTracker.startNewSession(
                        getString(BETA_MODE || DEV_MODE ? R.string.ga_tracking_beta : R.string.ga_tracking_market),
                        120 /* seconds */, this);
            }
        }

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
        mCloudApi.debugRequests = DEV_MODE;


        if (account != null) {
            FollowStatus.initialize(this, getCurrentUserId());
            Connections.initialize(this, "connections-"+getCurrentUserId());

            if (ContentResolver.getIsSyncable(account, AUTHORITY) < 1) {
                enableSyncing(account, SyncAdapterService.DEFAULT_SYNC_DELAY);
            }
            C2DMReceiver.register(this, getLoggedInUser());
        }

        if (BETA_MODE) {
            BetaService.scheduleCheck(this, false);
        }

        // make sure the WifiMonitor is disabled when not in beta mode
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, WifiMonitor.class),
                BETA_MODE ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP);
    }

    public synchronized User getLoggedInUser() {
        if (mLoggedInUser == null) {
            if (getCurrentUserId() != -1) {
                mLoggedInUser = SoundCloudDB.getUserById(getContentResolver(), getCurrentUserId());
            }
            if (mLoggedInUser == null) {
                mLoggedInUser = new User();
                mLoggedInUser.id = getAccountDataLong(User.DataKeys.USER_ID);
                mLoggedInUser.username = getAccountData(User.DataKeys.USERNAME);
                mLoggedInUser.primary_email_confirmed = getAccountDataBoolean(User.DataKeys.EMAIL_CONFIRMED);
            }
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

    public boolean isEmailConfirmed() {
        return getAccountDataBoolean(User.DataKeys.EMAIL_CONFIRMED);
    }

    public void confirmEmail() {
        setAccountData(User.DataKeys.EMAIL_CONFIRMED, true);
    }

    private ImageLoader createImageLoader() {
        final File cacheDir = CloudUtils.getCacheDir(this);
        ResponseCache cache = FileCache.install(cacheDir, Consts.MAX_IMAGE_CACHE);
        ContentHandler bitmapHandler;
        if (cache instanceof FileCache) {
            // workaround various SDK bugs by wrapping the handler
            bitmapHandler = FileCache.capture(new BitmapContentHandler(), null);
            FileCache.trim(cache);
        } else {
            // must be ICS - no workaround+trimming needed
            bitmapHandler = new BitmapContentHandler();
        }
        return new ImageLoader(ImageLoader.DEFAULT_TASK_LIMIT,
                null, /* streamFactory */
                bitmapHandler,
                null  /* prefetch handler */,
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

    public void onFrameUpdate(float maxAmplitude, long elapsed) {
        if (mRecListener != null) mRecListener.onFrameUpdate(maxAmplitude, elapsed);
    }

    public RecordListener getRecordListener() {
        return mRecListener;
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
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

    public boolean addUserAccount(User user, Token token) {
        final String type = getString(R.string.account_type);
        final Account account = new Account(user.username, type);
        final AccountManager am = getAccountManager();

        final boolean created = am.addAccountExplicitly(account, token.access, null);
        if (created) {
            am.setAuthToken(account, Token.ACCESS_TOKEN,  token.access);
            am.setAuthToken(account, Token.REFRESH_TOKEN, token.refresh);
            am.setUserData(account,  Token.SCOPE, token.scope);
            am.setUserData(account, User.DataKeys.USER_ID, Long.toString(user.id));
            am.setUserData(account, User.DataKeys.USERNAME, user.username);
            am.setUserData(account, User.DataKeys.EMAIL_CONFIRMED, Boolean.toString(
                    user.primary_email_confirmed));
        }
        // move this when we can't guarantee we will only have 1 account active at a time
        FollowStatus.initialize(this, user.id);

        enableSyncing(account, SyncAdapterService.DEFAULT_SYNC_DELAY);
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
            getAccountManager().setUserData(account, key, value);
            return true;
        }
    }

    public String suggestEmail() {
        Map<String,Integer> counts = new HashMap<String,Integer>();
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (CloudUtils.checkEmail(account.name)) {
                if (counts.get(account.name) == null) {
                    counts.put(account.name, 1);
                } else {
                    counts.put(account.name, counts.get(account.name) + 1);
                }
            }
        }
        if (counts.isEmpty()) {
            return null;
        } else {
            int max = 0;
            String candidate = null;
            for (Map.Entry<String,Integer> e : counts.entrySet()) {
                if (e.getValue() > max) {
                    max = e.getValue();
                    candidate = e.getKey();
                }
            }
            return candidate;
        }
    }

    public void trackPage(String path, Object... customVars) {
        if (mTracker != null && !TextUtils.isEmpty(path)) {
            try {
                if (customVars.length > 0 &&
                    customVars.length % 2 == 0) {
                    int slot=1;
                    for (int i=0; i<customVars.length; i+=2) {
                        Object key   = customVars[i];
                        Object value = customVars[i+1];
                        if (key == null) continue;
                        mTracker.setCustomVar(slot++, key.toString(), value != null ? value.toString() : "");
                        if (slot > 5) break; // max 5 slots
                    }
                }
                mTracker.trackPageView(path);
            } catch (IllegalStateException ignored) {
                // logs indicate this gets thrown occasionally
                Log.w(TAG, ignored);
            }
        }
    }

    public void trackEvent(String category, String action) {
        trackEvent(category, action, null, 0);
    }

    public void setCustomVar(int slot, String name, String value, int scope) {
        if (mTracker != null) {
            mTracker.setCustomVar(slot, name, value, scope);
        }
    }

    public void trackEvent(String category, String action, String label) {
        trackEvent(category, action, label, 0);
    }

    public void trackEvent(String category, String action, String label, int value) {
        if (mTracker != null && !TextUtils.isEmpty(category) && !TextUtils.isEmpty(action)) {
            mTracker.trackEvent(category, action, label, value);
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

    @Override
    public HttpClient getHttpClient() {
        return mCloudApi.getHttpClient();
    }

    @Override
    public Stream resolveStreamUrl(String uri, boolean skipLogging) throws IOException {
        return mCloudApi.resolveStreamUrl(uri, skipLogging);
    }

    @Override
    public String getUserAgent() {
        return mCloudApi.getUserAgent();
    }

    public void clearUserDbData() {
        getContentResolver().delete(Content.SEARCHES.uri, DBHelper.Searches.USER_ID + " = ?",new String[]{String.valueOf(getCurrentUserId())});
        getContentResolver().delete(Content.COLLECTIONS.uri, DBHelper.Collections.URI + " = ?", new String[]{Content.ME_TRACKS.toString()});
        getContentResolver().delete(Content.COLLECTIONS.uri, DBHelper.Collections.URI + " = ?", new String[]{Content.ME_FAVORITES.toString()});
        getContentResolver().delete(Content.COLLECTIONS.uri, DBHelper.Collections.URI + " = ?", new String[]{Content.ME_FOLLOWINGS.toString()});
        getContentResolver().delete(Content.COLLECTIONS.uri, DBHelper.Collections.URI + " = ?", new String[]{Content.ME_FOLLOWERS.toString()});
        Activities.clear(null, getContentResolver());
    }

    public void onFirstRun(int oldVersionCode, int newVersionCode) {
        for (int i = oldVersionCode; i < newVersionCode; ++i) {
            int nextVersion = i + 1;
            switch (nextVersion) {
                case 31:
                    if (getAccount() != null){ // enable syncing
                        enableSyncing(getAccount(), SyncAdapterService.DEFAULT_SYNC_DELAY);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static interface RecordListener {
        void onFrameUpdate(float maxAmplitude, long elapsed);
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