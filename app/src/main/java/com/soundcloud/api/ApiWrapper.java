package com.soundcloud.api;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface with SoundCloud, using OAuth2.
 * This API wrapper makes a few assumptions - namely:
 * <ul>
 * <li>Server responses are always requested in JSON format</li>
 * <li>Refresh-token handling is transparent to the client application (you should not need to
 * call <code>refreshToken()</code> manually)
 * </li>
 * <li>You use <a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpClient</a></li>
 * </ul>
 * Example usage:
 * <code>
 * <pre>
 * ApiWrapper wrapper = new ApiWrapper("client_id", "client_secret", null, null, Env.SANDBOX);
 * wrapper.login("login", "password");
 * HttpResponse response = wrapper.get(Request.to("/tracks"));
 *      </pre>
 * </code>
 *
 * @see <a href="http://developers.soundcloud.com/docs">Using the SoundCloud API</a>
 */
public class ApiWrapper implements CloudAPI {
    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    public static final int BUFFER_SIZE = 8192;
    /**
     * Connection timeout
     */
    public static final int TIMEOUT = 20 * 1000;
    /**
     * Keepalive timeout
     */
    public static final long KEEPALIVE_TIMEOUT = 20 * 1000;
    /* maximum number of connections allowed */
    public static final int MAX_TOTAL_CONNECTIONS = 10;
    private static final ThreadLocal<Request> defaultParams = new ThreadLocal<Request>() {
        @Override
        protected Request initialValue() {
            return new Request();
        }
    };
    /**
     * The current environment, only live possible for now
     */
    public final Env env = Env.LIVE;
    private final OAuth oAuth;
    private final AccountOperations accountOperations;
    /**
     * debug request details to stderr
     */
    public boolean debugRequests;
    transient private HttpClient httpClient;
    transient private CloudAPI.TokenListener listener;
    private String defaultContentType;
    private String defaultAcceptEncoding;

    /**
     * We do not want to use cookies, as it will result in continued sessions between logins / logouts
     */
    private static final CookieStore NO_OP_COOKIE_STORE = new CookieStore() {
        @Override
        public void addCookie(Cookie cookie) {

        }

        @Override
        public List<Cookie> getCookies() {
            return Collections.emptyList();
        }

        @Override
        public boolean clearExpired(Date date) {
            return false;
        }

        @Override
        public void clear() {

        }
    };

    public ApiWrapper(OAuth oAuth, AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
        this.oAuth = oAuth;
    }

    @Override
    public Token login(String username, String password) throws IOException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        final Request request = Request.to(Endpoints.TOKEN);
        addRequestParams(request, oAuth.getTokenRequestParamsFromUserCredentials(username, password));
        return requestToken(request);
    }

    private void addRequestParams(Request request, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Request an OAuth2 token from SoundCloud
     *
     * @param request the token request
     * @return the token
     * @throws java.io.IOException                               network error
     * @throws com.soundcloud.api.CloudAPI.InvalidTokenException unauthorized
     * @throws com.soundcloud.api.CloudAPI.ApiResponseException  http error
     */
    private Token requestToken(Request request) throws IOException {
        HttpResponse response = safeExecute(env.getSecureResourceHost(), request.buildRequest(HttpPost.class));
        final int status = response.getStatusLine().getStatusCode();

        String error;
        try {
            if (status == HttpStatus.SC_OK) {
                final Token token = new Token(Http.getJSON(response));
                if (listener != null) {
                    listener.onTokenRefreshed(token);
                }
                return token;
            } else {
                error = Http.getJSON(response).getString("error");
            }
        } catch (IOException ignored) {
            error = ignored.getMessage();
        } catch (JSONException ignored) {
            error = ignored.getMessage();
        }
        throw status == HttpStatus.SC_UNAUTHORIZED ?
                new CloudAPI.InvalidTokenException(status, error) :
                new CloudAPI.ApiResponseException(response, error);
    }



    @Override
    public Token clientCredentials(String... scopes) throws IOException {
        final Request req = Request.to(Endpoints.TOKEN);
        addRequestParams(req, oAuth.getTokenRequestParamsFromClientCredentials(scopes));

        final Token token = requestToken(req);
        if (scopes != null) {
            for (String scope : scopes) {
                if (!token.hasScope(scope)) {
                    throw new InvalidTokenException(-1, "Could not obtain requested scope '" + scope + "' (got: '" +
                            token.getScope() + "')");
                }
            }
        }
        return token;
    }

    @Override
    public Token extensionGrantType(String grantType) throws IOException {
        final Request req = Request.to(Endpoints.TOKEN);
        addRequestParams(req, oAuth.getTokenRequestParamsFromExtensionGrant(grantType));

        return requestToken(req);
    }

    @Override
    public Token refreshToken() throws IOException {
        final Token token = accountOperations.getSoundCloudToken();
        if (!token.hasRefreshToken()) {
            throw new IllegalStateException("no refresh token available");
        }
        final Request request = Request.to(Endpoints.TOKEN);
        addRequestParams(request, oAuth.getTokenRequestParamsForRefreshToken(token.getRefreshToken()));
        return requestToken(request);
    }

    @Nullable
    @Override
    public Token invalidateToken() {
        Token token = accountOperations.getSoundCloudToken();
        Token alternative = listener == null ? null : listener.onTokenInvalid(token);
        token.invalidate();
        if (alternative != null) {
            token = alternative;
            return token;
        } else {
            return null;
        }
    }

    /**
     * User-Agent to identify ourselves with - defaults to USER_AGENT
     *
     * @return the agent to use
     * @see CloudAPI#USER_AGENT
     */
    public String getUserAgent() {
        return USER_AGENT;
    }

    public URI getProxy() {
        Object proxy = getHttpClient().getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
        if (proxy instanceof HttpHost) {
            return URI.create(((HttpHost) proxy).toURI());
        } else {
            return null;
        }
    }

    /**
     * @param proxy the proxy to use for the wrapper, or null to clear the current one.
     */
    public void setProxy(URI proxy) {
        final HttpHost host;
        if (proxy != null) {
            Scheme scheme = getHttpClient()
                    .getConnectionManager()
                    .getSchemeRegistry()
                    .getScheme(proxy.getScheme());

            host = new HttpHost(proxy.getHost(), scheme.resolvePort(proxy.getPort()), scheme.getName());
        } else {
            host = null;
        }
        getHttpClient().getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, host);
    }

    public boolean isProxySet() {
        return getProxy() != null;
    }

    /**
     * @return The HttpClient instance used to make the calls
     */
    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public HttpClient getHttpClient() {
        if (httpClient == null) {
            final HttpParams params = getParams();
            HttpClientParams.setRedirecting(params, false);
            HttpProtocolParams.setUserAgent(params, getUserAgent());

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", getSocketFactory(), 80));
            final SSLSocketFactory sslFactory = getSSLSocketFactory();
            registry.register(new Scheme("https", sslFactory, 443));
            httpClient = new DefaultHttpClient(
                    new ThreadSafeClientConnManager(params, registry),
                    params) {
                {

                    setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
                        @Override
                        public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                            return KEEPALIVE_TIMEOUT;
                        }
                    });

                    getCredentialsProvider().setCredentials(
                            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, CloudAPI.REALM, OAUTH_SCHEME),
                            OAuth2Scheme.EmptyCredentials.INSTANCE);

                    getAuthSchemes().register(CloudAPI.OAUTH_SCHEME, new OAuth2Scheme.Factory(ApiWrapper.this));

                    addResponseInterceptor(new HttpResponseInterceptor() {
                        @Override
                        public void process(HttpResponse response, HttpContext context)
                                throws HttpException, IOException {
                            if (response == null || response.getEntity() == null) {
                                return;
                            }

                            HttpEntity entity = response.getEntity();
                            Header header = entity.getContentEncoding();
                            if (header != null) {
                                for (HeaderElement codec : header.getElements()) {
                                    if (codec.getName().equalsIgnoreCase("gzip")) {
                                        response.setEntity(new GzipDecompressingEntity(entity));
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }

                @Override
                protected CookieStore createCookieStore() {
                    return NO_OP_COOKIE_STORE;
                }

                @Override
                protected HttpContext createHttpContext() {
                    HttpContext ctxt = super.createHttpContext();
                    ctxt.setAttribute(ClientContext.AUTH_SCHEME_PREF,
                            Arrays.asList(CloudAPI.OAUTH_SCHEME, "digest", "basic"));
                    return ctxt;
                }

                @Override
                protected BasicHttpProcessor createHttpProcessor() {
                    BasicHttpProcessor processor = super.createHttpProcessor();
                    processor.addInterceptor(new OAuth2HttpRequestInterceptor());
                    return processor;
                }

                // for testability only
                @Override
                protected RequestDirector createClientRequestDirector(HttpRequestExecutor requestExec,
                                                                      ClientConnectionManager conman,
                                                                      ConnectionReuseStrategy reustrat,
                                                                      ConnectionKeepAliveStrategy kastrat,
                                                                      HttpRoutePlanner rouplan,
                                                                      HttpProcessor httpProcessor,
                                                                      HttpRequestRetryHandler retryHandler,
                                                                      RedirectHandler redirectHandler,
                                                                      AuthenticationHandler targetAuthHandler,
                                                                      AuthenticationHandler proxyAuthHandler,
                                                                      UserTokenHandler stateHandler,
                                                                      HttpParams params) {
                    return getRequestDirector(requestExec, conman, reustrat, kastrat, rouplan, httpProcessor, retryHandler,
                            redirectHandler, targetAuthHandler, proxyAuthHandler, stateHandler, params);
                }
            };
        }
        return httpClient;
    }

    @Override
    public long resolve(String url) throws IOException {
        HttpResponse resp = get(Request.to(Endpoints.RESOLVE).with("url", url));
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = resp.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                final String path = URI.create(location.getValue()).getPath();
                if (path != null && path.contains("/")) {
                    try {
                        final String id = path.substring(path.lastIndexOf('/') + 1);
                        return Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        throw new ResolverException(e, resp);
                    }
                } else {
                    throw new ResolverException("Invalid string:" + path, resp);
                }
            } else {
                throw new ResolverException("No location header", resp);
            }
        } else {
            throw new ResolverException("Invalid status code", resp);
        }
    }

    @Override
    public HttpResponse get(Request request) throws IOException {
        return execute(request, HttpGet.class);
    }

    @Override
    public HttpResponse put(Request request) throws IOException {
        return execute(request, HttpPut.class);
    }

    @Override
    public HttpResponse post(Request request) throws IOException {
        return execute(request, HttpPost.class);
    }

    @Override
    public HttpResponse delete(Request request) throws IOException {
        return execute(request, HttpDelete.class);
    }

    @Override
    public Token getToken() {
        return accountOperations.getSoundCloudToken();
    }

    public synchronized void setTokenListener(TokenListener listener) {
        this.listener = listener;
    }

    /**
     * Execute an API request, adds the necessary headers.
     *
     * @param request the HTTP request
     * @return the HTTP response
     * @throws java.io.IOException network error etc.
     */
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return safeExecute(env.getSecureResourceHost(), addHeaders(request));
    }

    // This design existed when the library was brought in to the project. Changing
    // it does not seem worthwhile, since the reassignment is once during validation
    // of inputs.
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        if (target == null) {
            target = determineTarget(request);
        }

        try {
            return getHttpClient().execute(target, request);
        } catch (NullPointerException e) {
            // this is a workaround for a broken httpclient version,
            // cf. http://code.google.com/p/android/issues/detail?id=5255
            // NPE in DefaultRequestDirector.java:456
            if (!request.isAborted() && request.getParams().isParameterFalse("npe-retried")) {
                request.getParams().setBooleanParameter("npe-retried", true);
                return safeExecute(target, request);
            } else {
                request.abort();
                throw new BrokenHttpClientException(e);
            }
        } catch (IllegalArgumentException e) {
            // more brokenness
            // cf. http://code.google.com/p/android/issues/detail?id=2690
            request.abort();
            throw new BrokenHttpClientException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Caused by: java.lang.ArrayIndexOutOfBoundsException: length=7; index=-9
            // org.apache.harmony.security.asn1.DerInputStream.readBitString(DerInputStream.java:72))
            // org.apache.harmony.security.asn1.ASN1BitString.decode(ASN1BitString.java:64)
            // ...
            // org.apache.http.conn.ssl.SSLSocketFactory.createSocket(SSLSocketFactory.java:375)
            request.abort();
            throw new BrokenHttpClientException(e);
        }
    }

    public String getDefaultContentType() {
        return (defaultContentType == null) ? DEFAULT_CONTENT_TYPE : defaultContentType;
    }

    public void setDefaultContentType(String contentType) {
        defaultContentType = contentType;
    }

    public String getDefaultAcceptEncoding() {
        return defaultAcceptEncoding;
    }

    public void setDefaultAcceptEncoding(String encoding) {
        defaultAcceptEncoding = encoding;
    }

    /**
     * Read wrapper from a file
     *
     * @param f the file
     * @return the wrapper
     * @throws IOException            IO problems
     * @throws ClassNotFoundException class not found
     */
    public static ApiWrapper fromFile(File f) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        try {
            return (ApiWrapper) ois.readObject();
        } finally {
            ois.close();
        }
    }

    /**
     * Adds a default parameter which will get added to all requests in this thread.
     * Use this method carefully since it might lead to unexpected side-effects.
     *
     * @param name  the name of the parameter
     * @param value the value of the parameter.
     */
    public static void setDefaultParameter(String name, String value) {
        defaultParams.get().set(name, value);
    }

    /**
     * Clears the default parameters.
     */
    public static void clearDefaultParameters() {
        defaultParams.remove();
    }

    /**
     * @return the default HttpParams
     * @see <a href="http://developer.android.com/reference/android/net/http/AndroidHttpClient.html#newInstance(java.lang.String, android.content.Context)">
     * android.net.http.AndroidHttpClient#newInstance(String, Context)</a>
     */
    protected HttpParams getParams() {
        final HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, BUFFER_SIZE);
        ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // fix contributed by Bjorn Roche XXX check if still needed
        params.setBooleanParameter("http.protocol.expect-continue", false);
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRoute() {
            @Override
            public int getMaxForRoute(HttpRoute httpRoute) {
                if (env.isApiHost(httpRoute.getTargetHost())) {
                    // there will be a lot of concurrent request to the API host
                    return MAX_TOTAL_CONNECTIONS;
                } else {
                    return ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
                }
            }
        });
        // apply system proxy settings
        final String proxyHost = System.getProperty("http.proxyHost");
        final String proxyPort = System.getProperty("http.proxyPort");
        if (proxyHost != null) {
            int port = 80;
            try {
                port = Integer.parseInt(proxyPort);
            } catch (NumberFormatException ignored) {
            }
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, port));
        }
        return params;
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

    // This design existed when the library was brought in to the project. Changing
    // it does not seem worthwhile, since the reassignment is once during validation
    // of inputs.
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    protected HttpResponse execute(Request req, Class<? extends HttpRequestBase> reqType) throws IOException {
        Request defaults = ApiWrapper.defaultParams.get();
        if (defaults != null && !defaults.getParams().isEmpty()) {
            // copy + merge in default parameters
            req = new Request(req);
            for (NameValuePair nvp : defaults) {
                req.add(nvp.getName(), nvp.getValue());
            }
        }
        logRequest(reqType, req);
        if (!req.getParams().containsKey(OAuth.PARAM_CLIENT_ID)) {
            req = new Request(req).add(OAuth.PARAM_CLIENT_ID, oAuth.getClientId());
        }
        return execute(req.buildRequest(reqType));
    }

    protected void logRequest(Class<? extends HttpRequestBase> reqType, Request request) {
        if (debugRequests) {
            System.err.println(reqType.getSimpleName() + " " + request);
        }
    }

    protected HttpHost determineTarget(HttpUriRequest request) {
        // A null target may be acceptable if there is a default target.
        // Otherwise, the null target is detected in the director.
        URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            return new HttpHost(
                    requestURI.getHost(),
                    requestURI.getPort(),
                    requestURI.getScheme());
        } else {
            return null;
        }
    }

    /**
     * Forces JSON
     */
    protected HttpUriRequest addAcceptHeader(HttpUriRequest request) {
        if (!request.containsHeader("Accept")) {
            request.addHeader("Accept", getDefaultContentType());
        }
        return request;
    }

    /**
     * Adds all required headers to the request
     */
    protected HttpUriRequest addHeaders(HttpUriRequest req) {
        HttpUriRequest request = addEncodingHeader(req);
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP) && getToken().valid()) {
            request.addHeader(AUTH.WWW_AUTH_RESP, oAuth.getAuthorizationHeaderValue());
        }
        return addAcceptHeader(request);
    }

    protected HttpUriRequest addEncodingHeader(HttpUriRequest req) {
        if (getDefaultAcceptEncoding() != null) {
            req.addHeader("Accept-Encoding", getDefaultAcceptEncoding());
        }
        return req;
    }

    /**
     * This method mainly exists to make the wrapper more testable. oh, apache's insanity.
     */
    protected RequestDirector getRequestDirector(HttpRequestExecutor requestExec,
                                                 ClientConnectionManager conman,
                                                 ConnectionReuseStrategy reustrat,
                                                 ConnectionKeepAliveStrategy kastrat,
                                                 HttpRoutePlanner rouplan,
                                                 HttpProcessor httpProcessor,
                                                 HttpRequestRetryHandler retryHandler,
                                                 RedirectHandler redirectHandler,
                                                 AuthenticationHandler targetAuthHandler,
                                                 AuthenticationHandler proxyAuthHandler,
                                                 UserTokenHandler stateHandler,
                                                 HttpParams params
    ) {
        return new DefaultRequestDirector(requestExec, conman, reustrat, kastrat, rouplan,
                httpProcessor, retryHandler, redirectHandler, targetAuthHandler, proxyAuthHandler,
                stateHandler, params);
    }

}
