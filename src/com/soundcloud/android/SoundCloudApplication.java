
package com.soundcloud.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.soundcloudapi.SoundCloudOptions;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.SoundCloudAuthorizationClient;
import com.soundcloud.utils.SoundCloudAuthorizationClient.AuthorizationStatus;
import com.soundcloud.utils.http.CountingMultipartRequestEntity;
import com.soundcloud.utils.http.ProgressListener;

@ReportsCrashes(formKey = "dHBndkdXY1lwUEN4QmtDZkNlQkh2YVE6MQ")
public class SoundCloudApplication extends Application {

    public static enum Events {
        track, favorite, playlist
    }

    private static final String TAG = "SoundCloudApplication";

    public static String PATH_MY_USERS = "me/followings";

    public static String PATH_MY_FEED = "events";

    public static String PATH_USERS = "users";

    public static String PATH_TRACKS = "tracks";

    public static String PATH_PLAYLISTS = "playlists";

    public static String PATH_MY_DETAILS = "me";

    public static String PATH_MY_ACTIVITIES = "me/activities/tracks";

    public static String PATH_MY_EXCLUSIVE_TRACKS = "me/activities/tracks/exclusive";

    public static String PATH_MY_TRACKS = "me/tracks";

    public static String PATH_MY_PLAYLISTS = "me/playlists";

    public static String PATH_MY_FAVORITES = "me/favorites";

    public static String PATH_MY_FOLLOWERS = "me/followers";

    public static String PATH_MY_FOLLOWINGS = "me/followings";

    public static String PATH_USER_DETAILS = "users/{user_id}";

    public static String PATH_USER_FOLLOWINGS = "users/{user_id}/followings";

    public static String PATH_USER_FOLLOWERS = "users/{user_id}/followers";

    public static String PATH_TRACK_DETAILS = "tracks/{track_id}";

    public static String PATH_USER_TRACKS = "users/{user_id}/tracks";

    public static String PATH_USER_FAVORITES = "users/{user_id}/favorites";

    public static String PATH_USER_PLAYLISTS = "users/{user_id}/playlists";

    public static String PATH_TRACK_COMMENTS = "tracks/{track_id}/comments";

    private SoundCloudAPI mSoundCloudApi;

    private ImageLoader mImageLoader;

    private static HttpClient httpClient;

    public static SoundCloudOptions sSoundCloudOptions =
    // SoundCloudAPI.USE_SANDBOX;
    SoundCloudAPI.USE_PRODUCTION;

    // SoundCloudAPI.USE_SANDBOX.with(OAuthVersion.V2_0);
    // SoundCloudAPI.USE_PRODUCTION.with(OAuthVersion.V2_0);

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        mImageLoader = createImageLoader(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSoundCloudApi = new SoundCloudAPI(getConsumerKey(), getConsumerSecret(), preferences
                .getString("oauth_access_token", ""), preferences.getString(
                "oauth_access_token_secret", ""), sSoundCloudOptions);
    }

    public SoundCloudAPI getSoundCloudAPI() {
        return mSoundCloudApi;
    }

    public SoundCloudAPI.State getState() {
        return mSoundCloudApi.getState();
    }

    public final void clearSoundCloudAccount() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().remove("oauth_access_token")
                .remove("oauth_access_token_secret").remove("lastDashboardIndex").remove(
                        "lastProfileIndex").putString("currentUserId", "-1").putString(
                        "currentUsername", "").commit();

        if (mSoundCloudApi != null) {
            mSoundCloudApi.unauthorize();
            mSoundCloudApi = null;
        }

        mSoundCloudApi = new SoundCloudAPI(getConsumerKey(), getConsumerSecret(), "", "",
                sSoundCloudOptions);
    }

    public void authorizeWithoutCallback(final SoundCloudAuthorizationClient client) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                AuthorizationStatus status = AuthorizationStatus.FAILED;

                try {
                    String url = mSoundCloudApi.obtainRequestToken("soundcloud://auth");
                    client.openAuthorizationURL(url);
                    String verificationCode = client.getVerificationCode();
                    if (verificationCode != null) {
                        mSoundCloudApi.obtainAccessToken(verificationCode);
                        status = AuthorizationStatus.SUCCESSFUL;
                        storeAuthorization();
                    }
                } catch (Exception e) {
                    client.exceptionOccurred(e);
                } finally {
                    final AuthorizationStatus finalStatus = status;
                    client.authorizationCompleted(finalStatus);
                    // complete(client, Thread.currentThread());
                }
            }
        });

        thread.start();
        // launch(client, thread);
    }

    private void storeAuthorization() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putString("oauth_access_token", mSoundCloudApi.getToken()).putString(
                "oauth_access_token_secret", mSoundCloudApi.getTokenSecret()).commit();
    }

    public String getConsumerKey() {
        return sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION ? this.getResources().getString(
                R.string.consumer_key) : this.getResources().getString(
                R.string.sandbox_consumer_key);
    }

    public String getConsumerSecret() {
        return sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION ? this.getResources().getString(
                R.string.consumer_secret) : this.getResources().getString(
                R.string.sandbox_consumer_secret);
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            HttpClient client = new DefaultHttpClient();
            httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(client.getParams(),
                    client.getConnectionManager().getSchemeRegistry()), client.getParams());
        }
        return httpClient;

    }

    public HttpUriRequest getPreparedRequest(String path) throws IllegalStateException,
            OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException,
            IOException, OAuthCommunicationException {

        HttpUriRequest req = getRequest(path, null);
        req.getParams().setParameter("consumer_key", getConsumerKey());
        req.addHeader("Accept", "application/json");

        return req;
    }

    public HttpUriRequest getRequest(String path) throws OAuthMessageSignerException,
            OAuthExpectationFailedException, OAuthCommunicationException {
        return getRequest(path, null);
    }

    public InputStream executeRequest(HttpUriRequest req) throws IllegalStateException, IOException {
        HttpResponse response = getHttpClient().execute(req);
        return response.getEntity().getContent();
    }

    public InputStream getContent(String path) throws IllegalStateException,
            OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException,
            IOException, OAuthCommunicationException {

        return executeRequest(getPreparedRequest(path));
    }

    public HttpUriRequest getRequest(String path, List<NameValuePair> params)
            throws OAuthMessageSignerException, OAuthExpectationFailedException,
            OAuthCommunicationException {
        return mSoundCloudApi.getRequest(path, params);
    }

    private String getDomain() {
        return sSoundCloudOptions == SoundCloudAPI.USE_PRODUCTION ? "http://api.soundcloud.com/"
                : "http://api.sandbox-soundcloud.com/";
    }

    public HttpResponse getHttpResponse(String path) {

        try {
            return mSoundCloudApi.get(path);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public String getSignedUrl(String path) {
        try {
            return mSoundCloudApi.signStreamUrl(urlEncode(path)) + "&consumer_key="
                    + getConsumerKey();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public String signStreamUrl(String path) {
        try {
            return mSoundCloudApi.signStreamUrl(path) + "&consumer_key=" + getConsumerKey();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public String signStreamUrlNaked(String path) {
        try {
            return mSoundCloudApi.signStreamUrl(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public InputStream putContent(String path) {

        try {
            return mSoundCloudApi.put(path).getEntity().getContent();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public InputStream deleteContent(String path) throws IllegalStateException,
            OAuthMessageSignerException, OAuthExpectationFailedException, ClientProtocolException,
            IOException, OAuthCommunicationException {
        return mSoundCloudApi.delete(path).getEntity().getContent();
    }

    public HttpResponse upload(ContentBody trackBody, List<NameValuePair> params,
            ProgressListener listener) throws OAuthMessageSignerException,
            OAuthExpectationFailedException, ClientProtocolException, IOException,
            OAuthCommunicationException {

        return upload(trackBody, null, params, listener);
    }

    public HttpResponse upload(ContentBody trackBody, ContentBody artworkBody,
            List<NameValuePair> params, ProgressListener listener)
            throws OAuthMessageSignerException, OAuthExpectationFailedException,
            ClientProtocolException, IOException, OAuthCommunicationException {
        HttpPost post = new HttpPost(urlEncode("tracks", null));
        // fix contributed by Bjorn Roche
        post.getParams().setBooleanParameter("http.protocol.expect-continue", false);

        MultipartEntity entity = new MultipartEntity();
        for (NameValuePair pair : params) {
            try {
                entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
            } catch (UnsupportedEncodingException e) {
            }
        }
        entity.addPart("track[asset_data]", trackBody);

        if (artworkBody != null)
            entity.addPart("track[artwork_data]", artworkBody);

        CountingMultipartRequestEntity countingEntity = new CountingMultipartRequestEntity(entity,
                listener);
        post.setEntity(countingEntity);
        // return api.upload(trackBody,params);
        return mSoundCloudApi.performRequest(post);
    }

    public String urlEncode(String resource) {
        return urlEncode(resource, null);
    }

    private String urlEncode(String resource, List<NameValuePair> params) {
        String resourceUrl;
        if (resource.startsWith("/")) {
            resourceUrl = getDomain() + resource.substring(1);
        } else {
            resourceUrl = resource.contains("://") ? resource : getDomain() + resource;
        }
        return params == null ? resourceUrl : resourceUrl + "?"
                + URLEncodedUtils.format(params, "UTF-8");
    }

    class StringBodyNoHeaders extends StringBody {
        public StringBodyNoHeaders(String value) throws UnsupportedEncodingException {
            super(value);
        }

        @Override
        public String getMimeType() {
            return null;
        }

        @Override
        public String getTransferEncoding() {
            return null;
        }
    }

    private HashMap<String, String[]> dbColumns = new HashMap<String, String[]>();

    public void setDBColumns(HashMap<String, String[]> dbColumns) {
        this.dbColumns = dbColumns;
    }

    public HashMap<String, String[]> getDBColumns() {
        return dbColumns;
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
        ContentHandler bitmapHandler = FileResponseCache.capture(new BitmapContentHandler(), null);

        // For pre-fetching, use a "sink" content handler so that the
        // the binary image data is captured by the cache without actually
        // parsing and loading the image data into memory. After pre-fetching,
        // the image data can be loaded quickly on-demand from the local cache.
        ContentHandler prefetchHandler = FileResponseCache.capture(FileResponseCache.sink(), null);

        // Perform callbacks on the main thread
        Handler handler = null;

        return new ImageLoader(streamFactory, bitmapHandler, prefetchHandler, handler);
    }

    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
    }

    // caches a playlsit to be delivered to the playback service
    private ArrayList<Parcelable> mPlaylistCache = null;

    public void cachePlaylist(ArrayList<Parcelable> playlistCache) {
        mPlaylistCache = playlistCache;
    }

    public void getPlaylistTrack() {

    }

    public ArrayList<Parcelable> flushCachePlaylist() {
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

}
