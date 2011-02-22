package com.soundcloud.utils;

import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.mapper.CloudDateFormat;
import com.soundcloud.utils.http.CountingMultipartRequestEntity;
import com.soundcloud.utils.http.ProgressListener;
import oauth.signpost.exception.OAuthException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            HttpClient client = new DefaultHttpClient();
            httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(client.getParams(),
                    client.getConnectionManager().getSchemeRegistry()), client.getParams());
        }
        return httpClient;
    }


    @Override
    public int resolve(String uri) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        // XXX WTF
        client.setRedirectHandler(new RedirectHandler() {
            @Override
            public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                return false;
            }

            @Override
            public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
                return null;
            }
        });

        HttpResponse resp = client.execute(getRequest("resolve?url=" +
                URLEncoder.encode(uri, "UTF-8"), null));

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = resp.getFirstHeader("Location");
            if (location != null) {
                String s = location.getValue();
                if (s.indexOf("/") != -1) {
                    return Integer.parseInt(s.substring(s.lastIndexOf("/") + 1, s.length()));
                }
            }
        }
        return -1;
    }

    @Override
    public HttpUriRequest getRequest(String path, List<NameValuePair> params) {
        try {
            HttpUriRequest req = mSoundCloudApi.getRequest(path, params);

            req.getParams().setParameter("consumer_key", mConsumerKey);
            req.addHeader("Accept", "application/json");
            return req;

        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        try {
            return mSoundCloudApi.performRequest(request);
        } catch (OAuthException e) {
            throw new IOException(e.getMessage());
        }
    }

    private String getDomain() {
        return production ? "http://api.soundcloud.com/" : "http://api.sandbox-soundcloud.com/";
    }


    @Override
    public HttpResponse getContent(String path) throws IOException {
        return getHttpClient().execute(getRequest(path, null));
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
    public HttpResponse putContent(String path, List<NameValuePair> params) throws IOException {
        try {
            return mSoundCloudApi.put(path, params);
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpResponse postContent(String path, List<NameValuePair> params) throws IOException {
        try {
            return mSoundCloudApi.post(path, params);
        } catch (OAuthException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpResponse deleteContent(String path) throws IOException {
        try {
            return mSoundCloudApi.delete(path);
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
        (new Thread() {
            @Override
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
        }).start();
    }

    @Override
    public void unauthorize() {
        mSoundCloudApi.unauthorize();
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

        final HttpPost post = new HttpPost(urlEncode(Enddpoints.TRACKS, null));
        // fix contributed by Bjorn Roche
        post.getParams().setBooleanParameter("http.protocol.expect-continue", false);

        MultipartEntity entity = new MultipartEntity();
        for (NameValuePair pair : params) {
            try {
                entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        entity.addPart(Params.ASSET_DATA, trackBody);
        if (artworkBody != null) entity.addPart(Params.ARTWORK_DATA, artworkBody);

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
