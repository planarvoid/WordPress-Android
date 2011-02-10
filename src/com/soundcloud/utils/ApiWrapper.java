package com.soundcloud.utils;

import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.mapper.CloudDateFormat;
import com.soundcloud.utils.http.CountingMultipartRequestEntity;
import com.soundcloud.utils.http.ProgressListener;
import oauth.signpost.exception.OAuthException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ApiWrapper implements CloudAPI {
    static final String TAG = CloudAPI.class.getSimpleName();
    private SoundCloudAPI mSoundCloudApi;
    private boolean production;
    private DefaultHttpClient httpClient;
    private String mConsumerKey;
    private ObjectMapper mMapper;


    public ApiWrapper() {
        this("invalid", "invalid", "invalid", "invalid", false);
    }

    public ApiWrapper(String consumerKey,
                      String consumerSecret,
                      String token,
                      String secret,
                      boolean production) {

        mSoundCloudApi = new SoundCloudAPI(
            consumerKey,
            consumerSecret,

            token,
            secret,

            production ? SoundCloudAPI.USE_PRODUCTION : SoundCloudAPI.USE_SANDBOX);

        this.production = production;
        this.mConsumerKey = consumerKey;
    }

    @Override
    public HttpUriRequest getPreparedRequest(String path) {
        HttpUriRequest req = getRequest(path, null);
        req.getParams().setParameter("consumer_key", mConsumerKey);
        req.addHeader("Accept", "application/json");
        return req;
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            HttpClient client = new DefaultHttpClient();
            httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(client.getParams(),
                    client.getConnectionManager().getSchemeRegistry()), client.getParams());
        }
        return httpClient;

    }

    @Override
    public InputStream executeRequest(String req) throws IOException {
        return executeRequest(getRequest(req, null));
    }

    @Override
    public InputStream executeRequest(HttpUriRequest req) throws IOException {
        HttpResponse response = getHttpClient().execute(req);
        return response.getEntity().getContent();
    }

    @Override
    public InputStream getContent(String path) throws IOException {
        return executeRequest(getPreparedRequest(path));
    }

    @Override
    public HttpUriRequest getRequest(String path, List<NameValuePair> params) {
        try {
            return mSoundCloudApi.getRequest(path, params);
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDomain() {
        return production ? "http://api.soundcloud.com/" : "http://api.sandbox-soundcloud.com/";
    }


    @Override
    public String getSignedUrl(String path) {
        try {
            return mSoundCloudApi.signStreamUrl(urlEncode(path)) + "&consumer_key="
                    + mConsumerKey;
        } catch (OAuthException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    @Override
    public String signStreamUrlNaked(String path) {
        try {
            return mSoundCloudApi.signStreamUrl(path);
        } catch (OAuthException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    @Override
    public InputStream putContent(String path) throws IOException {
        try {
            return mSoundCloudApi.put(path).getEntity().getContent();
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream deleteContent(String path) throws IOException {
        try {
            return mSoundCloudApi.delete(path).getEntity().getContent();
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SoundCloudAPI.State getState() {
        return mSoundCloudApi.getState();
    }


    @Override
    public void authorizeWithoutCallback(final Client client) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                SoundCloudAuthorizationClient.AuthorizationStatus status =
                        SoundCloudAuthorizationClient.AuthorizationStatus.FAILED;

                try {
                    String url = mSoundCloudApi.obtainRequestToken("soundcloud://auth");
                    client.openAuthorizationURL(url);
                    String verificationCode = client.getVerificationCode();
                    if (verificationCode != null) {
                        mSoundCloudApi.obtainAccessToken(verificationCode);
                        status = SoundCloudAuthorizationClient.AuthorizationStatus.SUCCESSFUL;
                        client.storeKeys(mSoundCloudApi.getToken(), mSoundCloudApi.getTokenSecret());
                    }
                } catch (Exception e) {
                    client.exceptionOccurred(e);
                } finally {
                    client.authorizationCompleted(status);
                }
            }
        });
        thread.start();
    }


    @Override
    public void unauthorize() {
    }

    public ObjectMapper getMapper() {
        if (this.mMapper == null) {
            mMapper = new ObjectMapper();
            mMapper.getDeserializationConfig().setDateFormat(CloudDateFormat.INSTANCE);
        }
        return mMapper;
    }

    @Override
    public HttpResponse upload(ContentBody trackBody,
                               ContentBody artworkBody,
                               List<NameValuePair> params,
                               ProgressListener listener)
            throws IOException {

        final HttpPost post = new HttpPost(urlEncode("tracks", null));
        // fix contributed by Bjorn Roche
        post.getParams().setBooleanParameter("http.protocol.expect-continue", false);

        MultipartEntity entity = new MultipartEntity();
        for (NameValuePair pair : params) {
            try {
                entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        entity.addPart("track[asset_data]", trackBody);
        if (artworkBody != null) entity.addPart("track[artwork_data]", artworkBody);

        post.setEntity(new CountingMultipartRequestEntity(entity, listener));

        try {
            return mSoundCloudApi.performRequest(post);
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
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


    static class StringBodyNoHeaders extends StringBody {
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
}
