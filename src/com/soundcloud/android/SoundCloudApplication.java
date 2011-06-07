package com.soundcloud.android;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.CloudCache;
import com.soundcloud.android.utils.LruCache;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.ObjectMapper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ContentHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ReportsCrashes(formKey = "dFVUNnFjazQ4RVFqOG1wcFl1MTU5QVE6MQ")
public class SoundCloudApplication extends Application implements AndroidCloudAPI, CloudAPI.TokenListener {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static final String GA_TRACKING = "UA-2519404-11";

    public static final boolean EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT);
    public static final boolean DALVIK = "Dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));

    public static boolean DEV_MODE;

    static final boolean API_PRODUCTION = true;

    private RecordListener mRecListener;

    private Wrapper mCloudApi;
    private List<Parcelable> mPlaylistCache;
    private ImageLoader mImageLoader;
    private final LruCache<Long, Track> mTrackCache = new LruCache<Long, Track>(32);
    private GoogleAnalyticsTracker tracker;

    public boolean playerWaitForArtwork;

    public static final LruCache<String, SoftReference<Bitmap>> bitmaps =
            new LruCache<String, SoftReference<Bitmap>>(256);
    public static final LruCache<String, Throwable> bitmapErrors =
            new LruCache<String, Throwable>(256);

    public boolean scrollTop;

    @Override
    public void onCreate() {
        super.onCreate();

        if (DALVIK) {
            if (!EMULATOR) {
                ACRA.init(this); // don't use ACRA when running unit tests / emulator
            }
            tracker = GoogleAnalyticsTracker.getInstance();
            tracker.start(GA_TRACKING, this);
        }

        createImageLoaders();
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
        DEV_MODE = false; // isDevMode();
        mCloudApi.debugRequests = DEV_MODE;
    }

    public void clearSoundCloudAccount(final Runnable success, final Runnable error) {
        mCloudApi.invalidateToken();

        Account account = getAccount();
        if (account != null) {
            getAccountManager().removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
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
            }, /*handler*/ null);
        }
    }

    public boolean isEmailConfirmed() {
        return getAccountDataBoolean(User.DataKeys.EMAIL_CONFIRMED);
    }

    public void confirmEmail() {
        setAccountData(User.DataKeys.EMAIL_CONFIRMED, true);
    }

    private void createImageLoaders() {
        CloudCache.install(this);
        ContentHandler mBitmapHandler = FileResponseCache.capture(new BitmapContentHandler(), null);
        ContentHandler prefetchHandler = FileResponseCache.capture(FileResponseCache.sink(), null);
        mImageLoader = new ImageLoader(null, mBitmapHandler, prefetchHandler, null);
    }

    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
    }

    public void cachePlaylist(ArrayList<Parcelable> playlistCache) {
        mPlaylistCache = playlistCache;
    }

    public List<Parcelable> flushCachePlaylist() {
        List<Parcelable> playlistRef = mPlaylistCache;
        mPlaylistCache = null;
        return playlistRef;
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

    public void cacheTrack(Track track) {
        mTrackCache.put(track.id, track);
    }

    public Track getTrackFromCache(long track_id) {
        return mTrackCache.get(track_id);
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
            am.setUserData(account, User.DataKeys.EMAIL_CONFIRMED, Boolean.toString(user.primary_email_confirmed));
        }

        // disable syncing
        ContentResolver.setIsSyncable(account, ScContentProvider.AUTHORITY, 0);
        return created;
    }

    public String getAccountData(String key) {
        Account account = getAccount();
        return account == null ? null : getAccountManager().getUserData(account, key);
    }

    public int getAccountDataInt(String key) {
        String data = getAccountData(key);
        return data == null ? 0 : Integer.parseInt(data);
    }

    public long getAccountDataLong(String key) {
        String data = getAccountData(key);
        return data == null ? 0 : Long.parseLong(data);
    }

    public boolean getAccountDataBoolean(String key) {
        String data = getAccountData(key);
        return data != null && Boolean.parseBoolean(data);
    }

    public long getCurrentUserId(){
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

    public void pageTrack(String path) {
        if (tracker != null && !TextUtils.isEmpty(path)) {
            try {
                tracker.trackPageView(path);
                tracker.dispatch();
            } catch (IllegalStateException ignored) {
                // logs indicate this gets thrown occasionally
                Log.w(TAG, ignored);
            }
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
        Token token = new Token(getAccessToken(account), getRefreshToken(account));
        token.scope = getAccountData(Token.SCOPE);
        return token;
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

    public HttpResponse get(Request resource) throws IOException {
        return mCloudApi.get(resource);
    }

    public Token clientCredentials() throws IOException {
        return mCloudApi.clientCredentials();
    }

    public Token clientCredentials(String scope) throws IOException {
        return mCloudApi.clientCredentials(scope);
    }

    public Token login(String username, String password) throws IOException {
        return mCloudApi.login(username, password);
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

    public Token authorizationCode(String code) throws IOException {
        return mCloudApi.authorizationCode(code);
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

    private boolean isDevMode() {
        try {
             PackageInfo info = getPackageManager().getPackageInfo(
                     getClass().getPackage().getName(),
                     PackageManager.GET_SIGNATURES);
            if (info != null && info.signatures != null) {
                String[] debugKeys = getResources().getStringArray(R.array.debug_sigs);
                String currentSignature =  info.signatures[0].toCharsString();
                Arrays.sort(debugKeys);
                if (Arrays.binarySearch(debugKeys, currentSignature) > -1) return true;
            }
        } catch (NameNotFoundException ignored) {}
        return false;
    }
}