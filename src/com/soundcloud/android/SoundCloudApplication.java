
package com.soundcloud.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.utils.ApiWrapper;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.LruCache;
import com.soundcloud.utils.http.ProgressListener;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReportsCrashes(formKey = "dDBFTG1DVkRYb2FMeXV1eE41SEo3Y3c6MQ")
public class SoundCloudApplication extends Application implements CloudAPI {
    public static final String TAG = SoundCloudApplication.class.getSimpleName();
    public static boolean EMULATOR = "google_sdk".equals(android.os.Build.PRODUCT) ||
                                     "sdk".equals(android.os.Build.PRODUCT);

    static final boolean API_PRODUCTION = true;

    private CloudAPI mCloudApi;
    private ArrayList<Parcelable> mPlaylistCache = null;
    private ImageLoader mImageLoader;
    static ContentHandler mBitmapHandler;

    public static enum Events {
        track, favorite, playlist
    }

    public static final Map<String, SoftReference<Bitmap>> mBitmaps =
            Collections.synchronizedMap(new LruCache<String, SoftReference<Bitmap>>());
    public static final Map<String, Throwable> mBitmapErrors =
            Collections.synchronizedMap(new LruCache<String, Throwable>());

    private HashMap<String, String[]> dbColumns = new HashMap<String, String[]>();

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        mImageLoader = createImageLoader(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mCloudApi = new ApiWrapper(
            getConsumerKey(API_PRODUCTION),
            getConsumerSecret(API_PRODUCTION),
            preferences.getString("oauth_access_token", ""),
            preferences.getString("oauth_access_token_secret", ""),
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
                .remove("oauth_access_token")
                .remove("oauth_access_token_secret")
                .remove("lastDashboardIndex")
                .remove("lastProfileIndex")
                .putString("currentUserId", "-1")
                .putString("currentUsername", "")
                .commit();


        mCloudApi.unauthorize();
    }

    public HashMap<String, String[]> getDBColumns() {
        return dbColumns;
    }
    
    public ContentHandler getBitmapHandler(){
        return mBitmapHandler;
    }

    /**
     * Create an instance of the imageloader. Library and examples of cacher and
     * code found at: {@link} http://code.google.com/p/libs-for-android/
     */
    private static ImageLoader createImageLoader(Context context) {
        // Install the file cache (if it is not already installed)
        CloudCache.install(context);

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

        return new ImageLoader(streamFactory, mBitmapHandler, prefetchHandler, handler);
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
        ArrayList<Parcelable> playlistRef = mPlaylistCache;
        mPlaylistCache = null;
        return playlistRef;
    }

    private RecordListener mRecListener = null;

    public void onFrameUpdate(float maxAmplitude) {
        if (mRecListener != null) {
            mRecListener.onFrameUpdate(maxAmplitude);
        }
    }

    public void setRecordListener(RecordListener listener) {
        this.mRecListener = listener;
    }

    // Define our custom Listener interface
    public interface RecordListener {
        public abstract void onFrameUpdate(float maxAmplitude);
    }


    public ObjectMapper getMapper() {
        return mCloudApi.getMapper();
    }

    public InputStream executeRequest(String req) throws IOException {
        return mCloudApi.executeRequest(req);
    }

    public InputStream getContent(String path) throws IOException {
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

    public InputStream putContent(String path, List<NameValuePair> params) throws IOException {
        return mCloudApi.putContent(path, params);
    }
    
    public HttpResponse postContent(String path, List<NameValuePair> params) throws IOException {
        return mCloudApi.postContent(path, params);
    }

    public InputStream deleteContent(String path) throws IOException {
        return mCloudApi.deleteContent(path);
    }

    public HttpResponse upload(ContentBody trackBody, ContentBody artworkBody, List<NameValuePair> params, ProgressListener listener) throws IOException {
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

    public HttpUriRequest getPreparedRequest(String path) {
        return mCloudApi.getPreparedRequest(path);
    }

    public InputStream executeRequest(HttpUriRequest req) throws IOException {
        return mCloudApi.executeRequest(req);
    }
}
