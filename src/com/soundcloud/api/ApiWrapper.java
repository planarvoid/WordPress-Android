package com.soundcloud.api;

import com.soundcloud.android.mapper.CloudDateFormat;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ApiWrapper implements CloudAPI {
    private ObjectMapper mMapper;

    private DefaultHttpClient httpClient;
    private String mClientId, mClientSecret;
    private String mToken, mRefreshToken;
    private final Env mEnv;
    private String mScope;
    private long mExpiresIn;
    private Set<TokenStateListener> listeners = new HashSet<TokenStateListener>();

    public ApiWrapper() {
        this("invalid", "invalid", "invalid", "invalid", Env.SANDBOX);
    }

    public ApiWrapper(String clientId,
                      String clientSecret,
                      String token,
                      String refreshToken,
                      Env env) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mToken = token;
        mRefreshToken = refreshToken;
        mEnv = env;
    }

    public ApiWrapper login(String username, String password) throws IOException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        Http.Params p = new Http.Params(
                "grant_type", "password",
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "username", username,
                "password", password);
        requestToken(p);
        return this;
    }

    @Override
    public void invalidateToken() {
        final String token = mToken;
        if (token != null) {
            for (TokenStateListener l : listeners) {
                l.onTokenInvalid(token);
            }
            mToken = null;
        }
    }

    public ApiWrapper refreshToken() throws IOException {
        if (mRefreshToken == null) throw new IllegalStateException("no refresh token available");
        Http.Params p = new Http.Params(
                "grant_type", REFRESH_TOKEN,
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "refresh_token", mRefreshToken);
        requestToken(p);
        return this;
    }

    private void requestToken(Http.Params params) throws IOException {
        HttpPost post = new HttpPost(CloudAPI.Enddpoints.TOKEN);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(params.queryString()));
        HttpResponse response = getHttpClient().execute(mEnv.sslHost, post);

        final int status = response.getStatusLine().getStatusCode();
        final String json = Http.getString(response);
        try {
            JSONObject resp = new JSONObject(json);

            switch (status) {
                case HttpStatus.SC_OK:
                    mToken = resp.getString(ACCESS_TOKEN);
                    mRefreshToken = resp.getString(REFRESH_TOKEN);
                    if (!resp.isNull(SCOPE)) mScope = resp.getString(SCOPE);
                    long expiresIn = resp.getLong(EXPIRES_IN);
                    mExpiresIn = System.currentTimeMillis() + expiresIn * 1000;
                    for (TokenStateListener l : listeners) {
                        l.onTokenRefreshed(mToken, mRefreshToken, mExpiresIn);
                    }
                    break;
                case HttpStatus.SC_UNAUTHORIZED:
                    String error = resp.getString("error");
                    throw new InvalidTokenException(status, error);
                default:
                    throw new IOException("HTTP error " + status + " " + resp.getString("error"));
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            final HttpParams defaults = Http.defaultParams();
            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            final SSLSocketFactory sslFactory = SSLSocketFactory.getSocketFactory();
            if (mEnv == Env.SANDBOX) sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registry.register(new Scheme("https", sslFactory, 443));
            httpClient = new DefaultHttpClient(
                    new ThreadSafeClientConnManager(defaults, registry),
                    defaults) {
                @Override protected HttpContext createHttpContext() {
                    HttpContext ctxt = super.createHttpContext();
                    ctxt.setAttribute(ClientContext.AUTH_SCHEME_PREF,
                            Arrays.asList(CloudAPI.OAUTH_SCHEME, "digest", "basic"));
                    return ctxt;
                }
                @Override protected BasicHttpProcessor createHttpProcessor() {
                    BasicHttpProcessor processor = super.createHttpProcessor();
                    processor.addInterceptor(new OAuthHttpRequestInterceptor());
                    return processor;
                }
            };
            // no redirects please
            httpClient.setRedirectHandler(new RedirectHandler() {
                @Override public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                    return false;
                }
                @Override public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
                    return null;
                }
            });
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, CloudAPI.REALM, OAUTH_SCHEME),
                    OAuthScheme.EmptyCredentials.INSTANCE);
            httpClient.getAuthSchemes().register(CloudAPI.OAUTH_SCHEME, new OAuthScheme.Factory(this));
        }
        return httpClient;
    }

    @Override
    public long resolve(String url) throws IOException {
        HttpResponse resp = getContent(Enddpoints.RESOLVE, new Http.Params("url", url));
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
    public String signUrl(String path) {
        return path + (path.contains("?") ? "&" : "?") + "oauth_token=" + getToken();
    }

    @Override
    public HttpResponse getContent(String resource) throws IOException {
        return getContent(resource, null);
    }

    @Override
    public HttpResponse getContent(String resource, Http.Params params) throws IOException {
        final HttpUriRequest req = new HttpGet(params == null ? resource : params.url(resource));
        req.addHeader("Accept", "application/json");
        return execute(req);
    }

    @Override
    public HttpResponse putContent(String resource, Http.Params params) throws IOException {
        return execute(new HttpPut(params == null ? resource : params.url(resource)));
    }

    @Override
    public HttpResponse postContent(String resource, Http.Params params) throws IOException {
        return execute(new HttpPost(params == null ? resource : params.url(resource)));
    }

    @Override
    public HttpResponse deleteContent(String resource) throws IOException {
        return execute(new HttpDelete(resource));
    }

    @Override
    public String getToken() {
        return mToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public String getScope() {
        return mScope;
    }

    public Date getExpiresIn() {
        return mExpiresIn == 0 ? null : new Date(mExpiresIn);
    }

    public void updateTokens(String access, String refresh) {
        mToken = access;
        mRefreshToken = refresh;
    }

    public void addTokenRefreshListener(TokenStateListener listener) {
        listeners.add(listener);
    }

    public HttpRequest addAuthorization(HttpRequest request) {
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
            request.addHeader(getOAuthHeader(getToken()));
        }
        return request;
    }

    public static Header getOAuthHeader(String token) {
        return new BasicHeader(AUTH.WWW_AUTH_RESP, "OAuth " + token);
    }

    public ObjectMapper getMapper() {
        if (this.mMapper == null) {
            mMapper = new ObjectMapper();
            mMapper.getDeserializationConfig().setDateFormat(CloudDateFormat.INSTANCE);
        }
        return mMapper;
    }

    @Override
    public HttpResponse uploadTrack(ContentBody trackBody,
                                    ContentBody artworkBody,
                                    Http.Params params,
                                    ProgressListener listener) throws IOException {
        final HttpPost post = new HttpPost(Enddpoints.TRACKS);
        MultipartEntity entity = new MultipartEntity();
        for (NameValuePair pair : params) {
            try {
                entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        entity.addPart(TrackParams.ASSET_DATA, trackBody);
        if (artworkBody != null) entity.addPart(TrackParams.ARTWORK_DATA, artworkBody);

        post.setEntity(new CountingMultipartRequestEntity(entity, listener));
        return execute(post);
    }

    private HttpResponse execute(HttpRequest req) throws IOException {
        return getHttpClient().execute(mEnv.sslHost, addAuthorization(req));
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
