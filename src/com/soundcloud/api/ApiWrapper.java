package com.soundcloud.api;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
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
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Interface with SoundCloud, using OAuth2.
 * This API wrapper makes a few assumptions - namely:
 * <ul>
 * <li>Only resource owner passwords credentials is supported</li>
 * <li>Server responses are always requested in JSON format</li>
 * <li>Refresh-token handling is transparent to the client application</li>
 * </ul>
 *
 * @author Jan Berkel <jan@soundcloud.com>
 * @version 1.0
 * @see CloudAPI
 */
public class ApiWrapper implements CloudAPI {
    public static final String INVALIDATED_TOKEN = "invalidated";

    private DefaultHttpClient httpClient;
    private String mClientId, mClientSecret;
    private Token mToken;
    private final Env mEnv;
    private Set<TokenStateListener> listeners = new HashSet<TokenStateListener>();
    private final URI mRedirectUri;

    /**
     * Constructs a new ApiWrapper instance.
     *
     * @param clientId     the application client id
     * @param clientSecret the application client secret
     * @param redirectUri  the registered redirect url, or null
     * @param token        an valid token, or null if not known
     * @param env          the environment to use (LIVE/SANDBOX)
     * @see <a href="https://github.com/soundcloud/api/wiki/02.1-OAuth-2">API documentation</a>
     */
    public ApiWrapper(String clientId,
                      String clientSecret,
                      URI redirectUri,
                      Token token,
                      Env env) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mRedirectUri = redirectUri;
        mToken = token == null ? new Token(null, null) : token;
        mEnv = env;
    }

    @Override public Token login(String username, String password) throws IOException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        mToken = requestToken(new Http.Params(
                "grant_type", PASSWORD,
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "username", username,
                "password", password));
        return mToken;
    }

    @Override public Token authorizationCode(String code) throws IOException {
        if (code == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        mToken = requestToken(new Http.Params(
                "grant_type", AUTHORIZATION_CODE,
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "redirect_uri", mRedirectUri,
                "code", code));
        return mToken;
    }

    @Override public Token signupToken() throws IOException {
        final Token signup = requestToken(new Http.Params(
                "grant_type", CLIENT_CREDENTIALS,
                "client_id", mClientId,
                "client_secret", mClientSecret));
        if (!signup.signupScoped()) {
            throw new InvalidTokenException(200, "Could not obtain signup scope (got: '" +
                    signup.scope + "')");
        }
        return signup;
    }

    @Override public Token refreshToken() throws IOException {
        if (mToken == null || mToken.refresh == null) throw new IllegalStateException("no refresh token available");
        mToken = requestToken(new Http.Params(
                "grant_type", REFRESH_TOKEN,
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "refresh_token", mToken.refresh));
        return mToken;
    }

    @Override public Token exchangeToken(String oauth1AccessToken) throws IOException {
        if (oauth1AccessToken == null) throw new IllegalArgumentException("need access token");
        mToken = requestToken(new Http.Params(
                "grant_type", OAUTH1_TOKEN,
                "client_id", mClientId,
                "client_secret", mClientSecret,
                "refresh_token", oauth1AccessToken));
        return mToken;
    }

    @Override public void invalidateToken() {
        if (mToken != null) {
            mToken.invalidate();
            for (TokenStateListener l : listeners) {
                l.onTokenInvalid(mToken);
            }
        }
    }

    public URI loginViaFacebook() {
        return getURI(
                CloudAPI.Enddpoints.FACEBOOK_LOGIN,
                new Http.Params(
                        "redirect_uri", mRedirectUri,
                        "client_id", mClientId,
                        "response_type", "code"
                ),
                true);
    }

    public URI getURI(String resource, Http.Params params, boolean ssl) {
        return URI.create(
                (ssl ? mEnv.sslHost : mEnv.host).toURI() + resource +
                        (params == null ? "" : "?" + params.queryString()));
    }

    protected Token requestToken(Http.Params params) throws IOException {
        HttpPost post = new HttpPost(CloudAPI.Enddpoints.TOKEN);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new StringEntity(params.queryString()));
        HttpResponse response = getHttpClient().execute(mEnv.sslHost, post);

        final int status = response.getStatusLine().getStatusCode();
        final String json = Http.getString(response);
        if (json == null || json.length() == 0) throw new IOException("JSON response is empty");
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
                    throw new IOException("HTTP error " + status + " " + resp.getString("error"));
            }
        } catch (JSONException e) {
            throw new IOException("could not parse JSON document: " +
                    (json.length() > 80 ? (json.substring(0, 79) + "...") : json));
        }
    }


    /**
     * @return parameters used by the underlying HttpClient
     */
    protected HttpParams getParams() {
        return Http.defaultParams();
    }

    /**
     * @return SocketFactory used by the underlying HttpClient
     */
    protected SocketFactory getSocketFactory() {
        return PlainSocketFactory.getSocketFactory();
    }

    /**
     * @return SSL SocketFactory used by the underlying HttpClient
     */
    protected SSLSocketFactory getSSLSocketFactory() {
        return SSLSocketFactory.getSocketFactory();
    }

    /**
     * User-Agent to identify ourselves with - defaults to USER_AGENT
     *
     * @return the agent to use
     * @see CloudAPI#USER_AGENT
     */
    protected String getUserAgent() {
        return USER_AGENT;
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            final HttpParams params = getParams();
            HttpClientParams.setRedirecting(params, false);
            HttpProtocolParams.setUserAgent(params, getUserAgent());

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", getSocketFactory(), 80));
            final SSLSocketFactory sslFactory = getSSLSocketFactory();
            if (mEnv == Env.SANDBOX) {
                // disable strict checks on sandbox XXX remove when certificate is fixed
                sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            }
            registry.register(new Scheme("https", sslFactory, 443));
            httpClient = new DefaultHttpClient(
                    new ThreadSafeClientConnManager(params, registry),
                    params) {
                {
                    setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
                        @Override
                        public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                            return 20; // seconds
                        }
                    });

                    getCredentialsProvider().setCredentials(
                        new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, CloudAPI.REALM, OAUTH_SCHEME),
                        OAuthScheme.EmptyCredentials.INSTANCE);

                    getAuthSchemes().register(CloudAPI.OAUTH_SCHEME, new OAuthScheme.Factory(ApiWrapper.this));
                }

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

    @Override public HttpResponse getContent(String resource) throws IOException {
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

    public HttpResponse execute(HttpRequest req) throws IOException {
        return getHttpClient().execute(mEnv.sslHost, addHeaders(req));
    }

    public static Header getOAuthHeader(Token token) {
        return new BasicHeader(AUTH.WWW_AUTH_RESP, "OAuth " +
                (token == null || !token.valid() ? INVALIDATED_TOKEN : token.access));
    }

    protected HttpRequest addAuthHeader(HttpRequest request) {
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
            request.addHeader(getOAuthHeader(getToken()));
        }
        return request;
    }

    protected HttpRequest addAcceptHeader(HttpRequest request) {
        if (!request.containsHeader("Accept")) {
            request.addHeader("Accept", "application/json");
        }
        return request;
    }

    protected HttpRequest addHeaders(HttpRequest req) {
        return addAcceptHeader(
                addAuthHeader(req));
    }
}
