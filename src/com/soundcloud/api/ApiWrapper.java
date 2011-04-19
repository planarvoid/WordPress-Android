package com.soundcloud.api;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ApiWrapper implements CloudAPI {
    public static final String INVALIDATED_TOKEN = "invalidated";

    private DefaultHttpClient httpClient;
    private String mClientId, mClientSecret;
    private Token mToken;
    private final Env mEnv;
    private Set<TokenStateListener> listeners = new HashSet<TokenStateListener>();

    /**
     * Constructs a new ApiWrapper instance.
     * @param clientId            the application client id
     * @param clientSecret        the application client secret
     * @param accessToken               an access token, or null if not known
     * @param refreshToken        an refresh token, or null if not known
     * @param env                 the environment to use (LIVE/SANDBOX)
     * @see <a href="https://github.com/soundcloud/api/wiki/02.1-OAuth-2">API documentation</a>
     */
    public ApiWrapper(String clientId,
                      String clientSecret,
                      Token token,
                      Env env) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mToken = token == null ? new Token(null, null) : token;
        mEnv = env;
    }

    @Override public Token login(String username, String password) throws IOException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        mToken = requestToken(new Http.Params(
            "grant_type",    PASSWORD,
            "client_id",     mClientId,
            "client_secret", mClientSecret,
            "username",      username,
            "password",      password));
        return mToken;
    }

    @Override
    public Token signupToken() throws IOException {
        return requestToken(new Http.Params(
            "grant_type",    CLIENT_CREDENTIALS,
            "client_id",     mClientId,
            "client_secret", mClientSecret));
    }

    @Override public Token refreshToken() throws IOException {
        if (mToken == null || mToken.refresh == null) throw new IllegalStateException("no refresh token available");
        mToken = requestToken(new Http.Params(
            "grant_type",    REFRESH_TOKEN,
            "client_id",     mClientId,
            "client_secret", mClientSecret,
            "refresh_token", mToken.refresh));
        return mToken;
    }

    @Override
    public Token exchangeToken(String oauth1AccessToken) throws IOException {
        if (oauth1AccessToken == null) throw new IllegalArgumentException("need access token");
        mToken = requestToken(new Http.Params(
            "grant_type",    OAUTH1_TOKEN,
            "client_id",     mClientId,
            "client_secret", mClientSecret,
            "refresh_token", oauth1AccessToken));
        return mToken;
    }

    @Override
    public void invalidateToken() {
        if (mToken != null) {
            mToken.invalidate();
            for (TokenStateListener l : listeners) {
                l.onTokenInvalid(mToken);
            }
        }
    }

    private Token requestToken(Http.Params params) throws IOException {
        HttpPost post = new HttpPost(CloudAPI.Enddpoints.TOKEN);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(params.queryString()));
        HttpResponse response = getHttpClient().execute(mEnv.sslHost, post);

        final int status = response.getStatusLine().getStatusCode();
        final String json = Http.getString(response);

        if (json == null) throw new IOException("JSON response is empty");
        try {
            JSONObject resp = new JSONObject(json);
            switch (status) {
                case HttpStatus.SC_OK:
                    final Token token = new Token(resp);
                    for (TokenStateListener l : listeners) {
                        l.onTokenRefreshed(token);
                    }
                    return token;
                case HttpStatus.SC_UNAUTHORIZED:
                    String error = resp.getString("error");
                    throw new InvalidTokenException(status, error);
                default:
                    throw new IOException("HTTP error "+status +" "+resp.getString("error"));
            }
        } catch (JSONException e) {
            throw new IOException("could not parse JSON document: " +
                    (json.length() > 80 ? (json.substring(0,79)+"...") : json));
        }
    }


    /** @return parameters used by the underlying HttpClient */
    protected HttpParams getParams() {
        return Http.defaultParams();
    }

    /** @return SocketFactory used by the underlying HttpClient */
    protected SocketFactory getSocketFactory() {
        return PlainSocketFactory.getSocketFactory();
    }

    /** @return SSL SocketFactory used by the underlying HttpClient */
    protected SSLSocketFactory getSSLSocketFactory() {
        return SSLSocketFactory.getSocketFactory();
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            final HttpParams params = getParams();
            // we handle redirects ourselves
            HttpClientParams.setRedirecting(params, false);
            HttpProtocolParams.setUserAgent(params, USER_AGENT);

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", getSocketFactory(), 80));
            final SSLSocketFactory sslFactory = getSSLSocketFactory();
            if (mEnv == Env.SANDBOX) {
                // disable strict checks on sandbox
                // XXX remove then certificate is fixed
                sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            }
            registry.register(new Scheme("https", sslFactory, 443));
            httpClient = new DefaultHttpClient(
                    new ThreadSafeClientConnManager(params, registry),
                    params) {
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
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, CloudAPI.REALM,
                                  OAUTH_SCHEME), OAuthScheme.EmptyCredentials.INSTANCE);
            httpClient.getAuthSchemes().register(CloudAPI.OAUTH_SCHEME, new OAuthScheme.Factory(this));
        }
        return httpClient;
    }

    @Override public long resolve(String url) throws IOException {
        HttpResponse resp = getContent(Enddpoints.RESOLVE, new Http.Params("url", url));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = resp.getFirstHeader("Location");
            if (location != null) {
                String s = location.getValue();
                if (s.indexOf("/") != -1) {
                    try {
                        return Integer.parseInt(s.substring(s.lastIndexOf("/") + 1, s.length()));
                    } catch (NumberFormatException ignored) {
                        // ignored
                    }
                }
            }
        }
        return -1;
    }

    @Override public String signUrl(String path) {
        return path + (path.contains("?") ? "&" : "?") + "oauth_token=" + getToken();
    }

    @Override
    public HttpResponse getContent(String resource) throws IOException {
        return getContent(resource, null);
    }

    @Override public HttpResponse getContent(String resource, Http.Params params) throws IOException {
        if (params == null) params = new Http.Params();
        return execute(params.buildRequest(HttpGet.class, resource));
    }

    @Override public HttpResponse putContent(String resource, Http.Params params) throws IOException {
        if (params == null) params = new Http.Params();
        return execute(params.buildRequest(HttpPut.class, resource));
    }

    @Override public HttpResponse postContent(String resource, Http.Params params) throws IOException {
        if (params == null) params = new Http.Params();
        return execute(params.buildRequest(HttpPost.class, resource));
    }

    @Override public HttpResponse deleteContent(String resource) throws IOException {
        return execute(new Http.Params().buildRequest(HttpDelete.class, resource));
    }

    @Override public Token getToken() {
        return mToken;
    }

    @Override public void setToken(Token newToken) {
        mToken = newToken;
    }

    @Override public void addTokenStateListener(TokenStateListener listener) {
        listeners.add(listener);
    }

    public static Header getOAuthHeader(Token token) {
        return new BasicHeader(AUTH.WWW_AUTH_RESP, "OAuth " +
                (token == null || !token.valid() ? INVALIDATED_TOKEN : token.access));
    }

    protected HttpRequest addAuthorization(HttpRequest request) {
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
            request.addHeader(getOAuthHeader(getToken()));
        }
        return request;
    }

    protected HttpRequest addAccept(HttpRequest request) {
        if (!request.containsHeader("Accept")) {
            request.addHeader("Accept", "application/json");
        }
        return request;
    }

    public HttpResponse execute(HttpRequest req) throws IOException {
        return getHttpClient().execute(mEnv.sslHost, addAccept(addAuthorization(req)));
    }
}
