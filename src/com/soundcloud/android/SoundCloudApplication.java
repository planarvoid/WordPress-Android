
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
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportsCrashes(formKey = "dEdsVnppQ0RyOS12d0lPa0dYWDZ4Wmc6MQ")
public class SoundCloudApplication extends Application implements CloudAPI {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static boolean EMULATOR = "google_sdk".equals(android.os.Build.PRODUCT) ||
                                     "sdk".equals(android.os.Build.PRODUCT);

    static final boolean API_PRODUCTION = true;

    public static final String USERNAME = "currentUsername";
    public static final String USER_ID  = "currentUserId";
    public static final String TOKEN    = "oauth_access_token";
    public static final String SECRET   = "oauth_access_token_secret";
    public static final String EMAIL_CONFIRMED = "email_confirmed";
    public static final String DASHBOARD_IDX = "lastDashboardIndex";
    public static final String PROFILE_IDX = "lastProfileIndex";

    private CloudAPI mCloudApi;
    private ArrayList<Parcelable> mPlaylistCache = null;
    private ImageLoader mImageLoader;
    private ImageLoader mBitmapLoader;

    static ContentHandler mBitmapHandler;

    public static enum Events {
        track, favorite, playlist
    }

    public static final Map<String, SoftReference<Bitmap>> mBitmaps =
            Collections.synchronizedMap(new LruCache<String, SoftReference<Bitmap>>());
    public static final Map<String, Throwable> mBitmapErrors =
            Collections.synchronizedMap(new LruCache<String, Throwable>());

    private static final HashMap<Long, SoftReference<ArrayList<Comment>>> mCommentSoftCache =
        new HashMap<Long, SoftReference<ArrayList<Comment>>>();

    private static final HashMap<Long, ArrayList<Comment>> mCommentCache =
        new HashMap<Long, ArrayList<Comment>>();

    private static HashMap<String, String[]> dbColumns = new HashMap<String, String[]>();

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
            preferences.getString(TOKEN, ""),
            preferences.getString(SECRET, ""),
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
                .remove(TOKEN)
                .remove(SECRET)
                .remove(EMAIL_CONFIRMED)
                .remove(DASHBOARD_IDX)
                .remove(PROFILE_IDX)
                .putLong(USER_ID, -1)
                .putString(USERNAME, "")
                .commit();


        mCloudApi.unauthorize();
    }

    public static HashMap<String, String[]> getDBColumns() {
        return dbColumns;
    }

    public ContentHandler getBitmapHandler(){
        return mBitmapHandler;
    }

    /**
     * Create an instance of the imageloader. Library and examples of cacher and
     * code found at: {@link} http://code.google.com/p/libs-for-android/
     */
    private void createImageLoaders() {
        // Install the file cache (if it is not already installed)
        CloudCache.install(this);

        // Just use the default URLStreamHandlerFactory because
        // it supports all of the required URI schemes (http).
        URLStreamHandlerFactory streamFactory = null;

        // Load images using a BitmapContentHandler
        // and cache the image data in the file cache.
        mBitmapHandler = FileResponseCache.capture(new BitmapContentHandler(), null);

        // For pre-fetching, use a "sink" content handler so that the
        // the binary image data is captured by the cache without actually
        // parsing and loading the image data into memory. After pre-fetching,
        // the image data can be loaded quickly on-demand from the local cache.
        ContentHandler prefetchHandler = FileResponseCache.capture(FileResponseCache.sink(), null);

        // Perform callbacks on the main thread
        Handler handler = null;

        mImageLoader = new ImageLoader(streamFactory, mBitmapHandler, prefetchHandler, handler);
        mBitmapLoader = new ImageLoader(streamFactory, mBitmapHandler, prefetchHandler, handler);
    }


    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        }   return super.getSystemService(name);

    }

    public void cachePlaylist(ArrayList<Parcelable> playlistCache) {
        mPlaylistCache = playlistCache;
    }

    public List<Parcelable> flushCachePlaylist() {
        if (mCommentCache.size() > 10)
            mCommentCache.clear();

        ArrayList<Parcelable> playlistRef = mPlaylistCache;
        mPlaylistCache = null;
        return playlistRef;
    }

    private RecordListener mRecListener = null;

    public void onFrameUpdate(float maxAmplitude, long elapsed) {
        if (mRecListener != null) {
            mRecListener.onFrameUpdate(maxAmplitude, elapsed);
        }
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
    }

    // Define our custom Listener interface
    public interface RecordListener {
        public abstract void onFrameUpdate(float maxAmplitude, long elapsed);
    }


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

    @Override
    public int resolve(String uri) throws IOException {
        return mCloudApi.resolve(uri);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return mCloudApi.execute(request);
    }

    public void cacheComments(long track_id, ArrayList<Comment> comments){
        mCommentCache.put(track_id, comments);
    }

    public void uncacheComments(long track_id){
        mCommentCache.remove(track_id);
    }


    public ArrayList<Comment> getCommentsFromCache(long track_id){
        if (mCommentCache.get(track_id) != null)
            return mCommentCache.get(track_id);
        else if (mCommentSoftCache.get(track_id) != null && mCommentSoftCache.get(track_id).get() != null){
            return mCommentSoftCache.get(track_id).get();
        } else
            return null;
    }
}
