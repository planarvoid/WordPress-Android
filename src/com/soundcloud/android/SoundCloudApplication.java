package com.soundcloud.android;

import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.LruCache;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudCache;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Http;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.jackson.map.ObjectMapper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ContentHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportsCrashes(formKey = "dF9FRzEzNnpENEVZdVRFbkNXUHYwLWc6MQ")
public class SoundCloudApplication extends Application implements AndroidCloudAPI {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();

    public static boolean EMULATOR = "google_sdk".equals(android.os.Build.PRODUCT) ||
            "sdk".equals(android.os.Build.PRODUCT);

    public static boolean DEV_MODE;

    static final boolean API_PRODUCTION = true;
    private RecordListener mRecListener;

    private AndroidCloudAPI mCloudApi;
    private List<Parcelable> mPlaylistCache;
    private ImageLoader mImageLoader;

    public boolean playerWaitForArtwork;

    public static final Map<String, SoftReference<Bitmap>> bitmaps =
            Collections.synchronizedMap(new LruCache<String, SoftReference<Bitmap>>());
    public static final Map<String, Throwable> bitmapErrors =
            Collections.synchronizedMap(new LruCache<String, Throwable>());

    private final Map<Long, List<Comment>> mCommentCache = new HashMap<Long, List<Comment>>();

    @Override
    public void onCreate() {
        super.onCreate();

        if (isRunningOnDalvik() && !EMULATOR) {
            ACRA.init(this); // don't use ACRA when running unit tests / emulator
        }

        createImageLoaders();
        final Account account = getAccount();
        mCloudApi = new Wrapper(
                getClientId(API_PRODUCTION),
                getClientSecret(API_PRODUCTION),
                account == null ? null : getAccessToken(account),
                account == null ? null : getRefreshToken(account),
                CloudAPI.Env.LIVE
        );

        mCloudApi.addTokenStateListener(new TokenStateListener() {
            @Override
            public void onTokenInvalid(String token) {
                getAccountManager().invalidateAuthToken(
                        getString(R.string.account_type),
                        token);
            }

            @Override
            public void onTokenRefreshed(String access, String refresh, long expiresIn) {
                Log.d(TAG, "onTokenRefreshed");

                Account account = getAccount();
                AccountManager am = getAccountManager();
                if (account != null && access != null && refresh != null) {
                    am.setPassword(account, access);
                    am.setAuthToken(account, CloudAPI.ACCESS_TOKEN, access);
                    am.setAuthToken(account, CloudAPI.REFRESH_TOKEN, refresh);
                    am.setUserData(account, CloudAPI.EXPIRES_IN, "" + expiresIn);
                }
            }
        });

        try {
            String[] debugKeys = getResources().getStringArray(R.array.debug_sigs);
            String currentSignature = getPackageManager().getPackageInfo("com.soundcloud.android",
                    PackageManager.GET_SIGNATURES).signatures[0].toCharsString();
            Arrays.sort(debugKeys);
            if (Arrays.binarySearch(debugKeys, currentSignature) > -1) DEV_MODE = true;

        } catch (NameNotFoundException e) {}
    }

    public void clearSoundCloudAccount(final Runnable success, final Runnable error) {
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


        mCloudApi.invalidateToken();
        mCloudApi.updateTokens(null, null);
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
        if (mCommentCache.size() > 10) mCommentCache.clear();
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

    public void cacheComments(long track_id, List<Comment> comments) {
        mCommentCache.put(track_id, comments);
    }

    public void uncacheComments(long track_id) {
        mCommentCache.remove(track_id);
    }

    public List<Comment> getCommentsFromCache(long track_id) {
        return mCommentCache.get(track_id);
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
                CloudAPI.ACCESS_TOKEN, null, null, activity, callback, null);
    }

    public boolean addUserAccount(User user, String... tokens) {
        final String type = getString(R.string.account_type);
        final Account account = new Account(user.username, type);
        final AccountManager am = getAccountManager();
        final boolean created = am.addAccountExplicitly(account, tokens[0], null);
        if (created) {
            am.setAuthToken(account, CloudAPI.ACCESS_TOKEN,  tokens[0]);
            am.setAuthToken(account, CloudAPI.REFRESH_TOKEN, tokens[1]);
            am.setUserData(account, User.DataKeys.USER_ID, Long.toString(user.id));
            am.setUserData(account, User.DataKeys.USERNAME, user.username);
            am.setUserData(account, User.DataKeys.EMAIL_CONFIRMED, Boolean.toString(user.primary_email_confirmed));
        }
        return created;
    }

    public void useAccount(Account account) {
        mCloudApi.updateTokens(getAccessToken(account), getRefreshToken(account));
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

    public boolean setAccountData(String key, String value) {
        Account account = getAccount();
        if (account == null) {
            return false;
        } else {
            getAccountManager().setUserData(account, key, value);
            return true;
        }
    }

    private String getClientId(boolean production) {
        return getResources().getString(production ?
                R.string.consumer_key :
                R.string.sandbox_consumer_key);
    }

    private String getClientSecret(boolean production) {
        return getResources().getString(production ?
                R.string.consumer_secret :
                R.string.sandbox_consumer_secret);
    }

    private String getAccessToken(Account account) {
        return getAccountManager().getPassword(account);
    }

    private String getRefreshToken(Account account) {
        AccountManagerFuture<Bundle> bundle =
                getAccountManager().getAuthToken(account, CloudAPI.REFRESH_TOKEN, false, null, null);

        if (bundle.isDone()) {
            try {
                return bundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            } catch (OperationCanceledException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (AuthenticatorException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private AccountManager getAccountManager() {
        return AccountManager.get(this);
    }

    public HttpResponse getContent(String resource) throws IOException {
        return mCloudApi.getContent(resource);
    }

    public HttpResponse getContent(String resource, Http.Params params) throws IOException {
        return mCloudApi.getContent(resource, params);
    }

    public CloudAPI login(String username, String password) throws IOException {
        return mCloudApi.login(username, password);
    }

    public String signUrl(String path) {
        return mCloudApi.signUrl(path);
    }

    public HttpResponse putContent(String resource, Http.Params params) throws IOException {
        return mCloudApi.putContent(resource, params);
    }

    public HttpResponse postContent(String resource, Http.Params params) throws IOException {
        return mCloudApi.postContent(resource, params);
    }

    public HttpResponse deleteContent(String resource) throws IOException {
        return mCloudApi.deleteContent(resource);
    }

    public HttpResponse uploadTrack(ContentBody trackBody, ContentBody artworkBody, Http.Params params, ProgressListener listener) throws IOException {
        return mCloudApi.uploadTrack(trackBody, artworkBody, params, listener);
    }

    public CloudAPI refreshToken() throws IOException {
        return mCloudApi.refreshToken();
    }

    public String getToken() {
        return mCloudApi.getToken();
    }

    public String getRefreshToken() {
        return mCloudApi.getRefreshToken();
    }

    public long resolve(String uri) throws IOException {
        return mCloudApi.resolve(uri);
    }

    public void updateTokens(String access, String refresh) {
        mCloudApi.updateTokens(access, refresh);
    }

    public void addTokenStateListener(TokenStateListener listener) {
        mCloudApi.addTokenStateListener(listener);
    }

    public CloudAPI exchangeToken(String oauth1AccessToken) throws IOException {
        return mCloudApi.exchangeToken(oauth1AccessToken);
    }

    public void invalidateToken() {
        mCloudApi.invalidateToken();
    }

    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
    }

    public static boolean isRunningOnDalvik() {
        return "Dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));
    }

    public static interface RecordListener {
        void onFrameUpdate(float maxAmplitude, long elapsed);
    }
}
