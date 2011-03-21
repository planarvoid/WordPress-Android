
package com.soundcloud.android;

import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.LruCache;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.utils.ApiWrapper;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.http.Http;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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

    public static interface Prefs {
        String USERNAME = "currentUsername";
        String USER_ID  = "currentUserId";
        String TOKEN    = "oauth_access_token";
        String SECRET   = "oauth_access_token_secret";
        String EMAIL_CONFIRMED = "email_confirmed";
        String DASHBOARD_IDX = "lastDashboardIndex";
        String PROFILE_IDX = "lastProfileIndex";
    }

    private CloudAPI mCloudApi;
    private List<Parcelable> mPlaylistCache = null;
    private ImageLoader mImageLoader;

    public boolean playerWaitForArtwork;

    public static final Map<String, SoftReference<Bitmap>> bitmaps =
            Collections.synchronizedMap(new LruCache<String, SoftReference<Bitmap>>());
    public static final Map<String, Throwable> bitmapErrors =
            Collections.synchronizedMap(new LruCache<String, Throwable>());

    private static final Map<Long, List<Comment>> mCommentCache = new HashMap<Long, List<Comment>>();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            ACRA.init(this);
        } catch (Exception ignored) {
            Log.e(TAG, "error", ignored);
        }

        createImageLoaders();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCloudApi = new ApiWrapper(
            getConsumerKey(API_PRODUCTION),
            getConsumerSecret(API_PRODUCTION),
            preferences.getString(Prefs.TOKEN, ""),
            preferences.getString(Prefs.SECRET, ""),
            API_PRODUCTION);
    }

    protected String getConsumerKey(boolean production) {
        return getResources().getString(production ?
                R.string.consumer_key :
                R.string.sandbox_consumer_key);
    }

    protected String getConsumerSecret(boolean production) {
          return getResources().getString(production ?
                R.string.consumer_secret :
                R.string.sandbox_consumer_secret);
    }

    public void clearSoundCloudAccount() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .remove(Prefs.TOKEN)
                .remove(Prefs.SECRET)
                .remove(Prefs.EMAIL_CONFIRMED)
                .remove(Prefs.DASHBOARD_IDX)
                .remove(Prefs.PROFILE_IDX)
                .putLong(Prefs.USER_ID, -1)
                .putString(Prefs.USERNAME, "")
                .commit();

        mCloudApi.unauthorize();
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

    private RecordListener mRecListener = null;

    public void onFrameUpdate(float maxAmplitude, long elapsed) {
        if (mRecListener != null) mRecListener.onFrameUpdate(maxAmplitude, elapsed);
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
    }

    public interface RecordListener {
         void onFrameUpdate(float maxAmplitude, long elapsed);
    }


    public void cacheComments(long track_id, List<Comment> comments){
        mCommentCache.put(track_id, comments);
    }

    public void uncacheComments(long track_id){
        mCommentCache.remove(track_id);
    }

    public List<Comment> getCommentsFromCache(long track_id) {
        return mCommentCache.get(track_id);
    }

    // cloud api delegation
    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
    }

    public HttpResponse getContent(String path) throws IOException {
        return mCloudApi.getContent(path);
    }

    public HttpUriRequest getRequest(String path, List<NameValuePair> params) {
        return mCloudApi.getRequest(path, params);
    }

    public String getSignedUrl(String path) {
        return mCloudApi.getSignedUrl(path);
    }

    public String signStreamUrlNaked(String path) {
        return mCloudApi.signStreamUrlNaked(path);
    }

    public HttpResponse putContent(String path, List<NameValuePair> params) throws IOException {
        return mCloudApi.putContent(path, params);
    }

    public HttpResponse postContent(String path, List<NameValuePair> params) throws IOException {
        return mCloudApi.postContent(path, params);
    }

    public HttpResponse deleteContent(String path) throws IOException {
        return mCloudApi.deleteContent(path);
    }

    public HttpResponse upload(ContentBody trackBody, ContentBody artworkBody, List<NameValuePair> params, Http.ProgressListener listener) throws IOException {
        return mCloudApi.upload(trackBody, artworkBody, params, listener);
    }

    public void unauthorize() {
        mCloudApi.unauthorize();
    }

    public void authorizeWithoutCallback(CloudAPI.Client client) {
        mCloudApi.authorizeWithoutCallback(client);
    }

    public SoundCloudAPI.State getState() {
        return mCloudApi.getState();
    }

    public int resolve(String uri) throws IOException {
        return mCloudApi.resolve(uri);
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return mCloudApi.execute(request);
    }
}
