package com.soundcloud.android;

import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.LruCache;
import com.soundcloud.android.activity.EmailConfirm;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Http;
import com.soundcloud.android.utils.CloudCache;
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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ContentHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportsCrashes(formKey = "dF9FRzEzNnpENEVZdVRFbkNXUHYwLWc6MQ")
public class SoundCloudApplication extends Application implements CloudAPI {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static boolean EMULATOR = "google_sdk".equals(android.os.Build.PRODUCT) ||
            "sdk".equals(android.os.Build.PRODUCT);

    static final boolean API_PRODUCTION = true;
    private RecordListener mRecListener;

    private CloudAPI mCloudApi;
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

        mCloudApi = new ApiWrapper(
                getClientId(API_PRODUCTION),
                getClientSecret(API_PRODUCTION),
                account == null ? null : getAccessToken(account),
                account == null ? null : getRefreshToken(account),
                CloudAPI.Env.LIVE
        );

        mCloudApi.addTokenRefreshListener(new TokenStateListener() {
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

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .remove(Prefs.TOKEN)
                .remove(Prefs.SECRET)
                .remove(Prefs.EMAIL_CONFIRMED)
                .remove(Prefs.DASHBOARD_IDX)
                .remove(Prefs.PROFILE_IDX)
                .remove(EmailConfirm.PREF_LAST_REMINDED)
                .putLong(Prefs.USER_ID, -1)
                .putString(Prefs.USERNAME, "")
                .commit();

        mCloudApi.invalidateToken();
        mCloudApi.updateTokens(null, null);
    }

    public boolean isEmailConfirmed() {
        return PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(Prefs.EMAIL_CONFIRMED, false);
    }

    public void confirmEmail() {
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Prefs.EMAIL_CONFIRMED, true).commit();
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

    public void useAccount(Account account) {
        mCloudApi.updateTokens(getAccessToken(account), getRefreshToken(account));
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

    // cloud api delegation
    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
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

    public void addTokenRefreshListener(TokenStateListener listener) {
        mCloudApi.addTokenRefreshListener(listener);
    }

    public void invalidateToken() {
        mCloudApi.invalidateToken();
    }

    public static boolean isRunningOnDalvik() {
        return "Dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"));
    }

    public static interface Prefs {
        String USERNAME = "currentUsername";
        String USER_ID = "currentUserId";
        String TOKEN = "oauth_access_token";
        String SECRET = "oauth_access_token_secret";
        String EMAIL_CONFIRMED = "email_confirmed";
        String DASHBOARD_IDX = "lastDashboardIndex";
        String PROFILE_IDX = "lastProfileIndex";
    }

    public static interface RecordListener {
        void onFrameUpdate(float maxAmplitude, long elapsed);
    }
}
