package com.soundcloud.android.api.legacy;

import static android.util.Log.INFO;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PublicApi {

    /**
     * the parameter which we use to tell the API that this is a non-interactive request (e.g. background syncing.
     */
    public static final String BACKGROUND_PARAMETER = "_behavior[non_interactive]";

    public static final String LINKED_PARTITIONING = "linked_partitioning";
    public static final String TAG = PublicApi.class.getSimpleName();
    // other constants
    public static final String REALM = "SoundCloud";
    public static final String OAUTH_SCHEME = "oauth";
    public static final String VERSION = "1.3.1";
    public static final String USER_AGENT = "SoundCloud Java Wrapper (" + PublicApi.VERSION + ")";
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
    protected static final ThreadLocal<Request> defaultParams = new ThreadLocal<Request>() {
        @Override
        protected Request initialValue() {
            return new Request();
        }
    };
    private static final int API_LOOKUP_BATCH_SIZE = 200;
    private static final String UNCHECKED = "unchecked";
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
    private static PublicApi instance;
    /**
     * The current environment, only live possible for now
     */
    public final Env env = Env.LIVE;
    protected final OAuth oAuth;
    /**
     * debug request details to stderr
     */
    public boolean debugRequests;
    protected String defaultContentType;
    private ApplicationProperties applicationProperties;
    private ObjectMapper objectMapper;
    private Context context;
    private String userAgent;
    private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private AccountOperations accountOperations;
    transient private HttpClient httpClient;
    transient private TokenListener listener;
    private String defaultAcceptEncoding;

    @Deprecated
    public PublicApi(Context context) {
        this(context,
                SoundCloudApplication.fromContext(context).getAccountOperations(),
                new ApplicationProperties(context.getResources()), new BuildHelper());

    }

    @Deprecated
    public PublicApi(Context context, AccountOperations accountOperations,
                     ApplicationProperties applicationProperties, BuildHelper buildHelper) {
        this(context, buildObjectMapper(), new OAuth(accountOperations),
                accountOperations, applicationProperties,
                UnauthorisedRequestRegistry.getInstance(context), new DeviceHelper(context, buildHelper));
    }

    @VisibleForTesting
    PublicApi(Context context, ObjectMapper mapper, OAuth oAuth,
              AccountOperations accountOperations, ApplicationProperties applicationProperties,
              UnauthorisedRequestRegistry unauthorisedRequestRegistry,
              DeviceHelper deviceHelper) {
        this.accountOperations = accountOperations;
        this.oAuth = oAuth;
        // context can be null in tests
        if (context == null) {
            return;
        }
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.applicationProperties = applicationProperties;
        this.context = context;
        objectMapper = mapper;
        setTokenListener(new SoundCloudTokenListener(accountOperations));
        userAgent = deviceHelper.getUserAgent();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Actions.CHANGE_PROXY_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String proxy = intent.getStringExtra(Actions.EXTRA_PROXY);
                Log.d(TAG, "proxy changed: " + proxy);
                setProxy(proxy == null ? null : URI.create(proxy));
            }
        }, filter);

        if (applicationProperties.shouldEnableNetworkProxy()) {
            //The only place this const is used us here? Do we even set this at all?
            final String proxy =
                    PreferenceManager.getDefaultSharedPreferences(context).getString(Consts.PrefKeys.DEV_HTTP_PROXY, null);
            setProxy(TextUtils.isEmpty(proxy) ? null : URI.create(proxy));
        }

        setDefaultAcceptEncoding("gzip");
    }

    public synchronized static PublicApi getInstance(Context context) {
        if (instance == null) {
            instance = new PublicApi(context.getApplicationContext());
        }
        return instance;
    }

    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(new CloudDateFormat());
    }

    public static String generateRequestResponseLog(HttpHost target, HttpUriRequest request, @Nullable HttpResponse response) {
        StringBuilder sb = new StringBuilder(2000);
        sb.append(request.getMethod())
                .append(' ')
                .append(target.getSchemeName())
                .append(':')
                .append(target.toHostString())
                .append(' ')
                .append(request.getURI())
                .append(";headers=");
        final Header[] headers = request.getAllHeaders();
        for (Header header : headers) {
            sb.append(header.toString()).append(';');
        }
        sb.append("response=").append(response == null ? "NULL" : response.getStatusLine());
        return sb.toString();
    }

    /**
     * @param enabled if true, all requests will be tagged as coming from a background process
     */
    public static void setBackgroundMode(boolean enabled) {
        if (enabled) {
            setDefaultParameter(BACKGROUND_PARAMETER, "1");
        } else {
            clearDefaultParameters();
        }
    }

    public static boolean isStatusCodeOk(int code) {
        return code >= 200 && code < 400;
    }

    public static boolean isStatusCodeClientError(int code) {
        return code >= 400 && code < 500;
    }

    // accepts all certificates - don't use in production
    private static SSLSocketFactory unsafeSocketFactory() {
        try {
            final SSLContext context = SSLContext.getInstance("TLS");
            return new SSLSocketFactory(null, null, null, null, null, null) {
                {
                    context.init(null, new TrustManager[]{new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
                            Log.w(TAG, "trusting " + Arrays.asList(chain));
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
                            Log.w(TAG, "trusting " + Arrays.asList(chain));
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }}, null);
                }

                @Override
                public Socket createSocket() throws IOException {
                    return context.getSocketFactory().createSocket();
                }

                @Override
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return context.getSocketFactory().createSocket(socket, host, port, autoClose);
                }
            };
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
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

        if (applicationProperties.shouldEnableNetworkProxy()) {
            getHttpClient().getConnectionManager().getSchemeRegistry()
                    .register(new Scheme("https",
                            proxy != null ? unsafeSocketFactory() : getSSLSocketFactory(), 443));
        }

    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    protected SSLSocketFactory getSSLSocketFactory() {
        //Why do we do this differentiation? Why not just use the standard one?
        if (applicationProperties.isRunningOnDevice()) {
            // make use of android's implementation
            return SSLCertificateSocketFactory.getHttpSocketFactory(TIMEOUT,
                    new SSLSessionCache(context));
        } else {
            // httpclient default
            return SSLSocketFactory.getSocketFactory();
        }
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

    // add a bunch of logging in debug mode to make it easier to see and debug API request
    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        // sends the request
        HttpResponse response = null;
        try {
            response = safeExecute2TheSequel(target, request);
            recordUnauthorisedRequestIfRequired(response);
        } finally {
            logRequest(target, request, response);
        }

        return response;
    }

    private void recordUnauthorisedRequestIfRequired(HttpResponse response) {
        if (responseIsUnauthorised(response)) {
            if (accountOperations.hasValidToken()) {
                unauthorisedRequestRegistry.updateObservedUnauthorisedRequestTimestamp();
            }
        }
    }

    private void logRequest(HttpHost target, HttpUriRequest request, @Nullable HttpResponse response) {
        String report = generateRequestResponseLog(target, request, response);
        ErrorUtils.log(Log.INFO, TAG, report);
    }

    private boolean responseIsUnauthorised(HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PublicApiResource> T read(Request request) throws IOException {
        InputStream inputStream = getInputStream(get(request), request);
        try {
            return (T) getMapper().readValue(inputStream, PublicApiResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PublicApiResource> T update(Request request) throws IOException {
        InputStream inputStream = getInputStream(put(request), request);
        try {
            return (T) getMapper().readValue(inputStream, PublicApiResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PublicApiResource> T create(Request request) throws IOException {
        InputStream inputStream = getInputStream(post(request), request);
        try {
            return (T) getMapper().readValue(inputStream, PublicApiResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @SuppressWarnings(UNCHECKED)
    @NotNull
    public <T extends PublicApiResource> List<T> readList(Request request) throws IOException {
        JsonParser parser = null;
        List<T> result;
        InputStream is = getInputStream(get(request), request);

        try {
            parser = getMapper().getFactory().createParser(is);
            JsonToken t = parser.getCurrentToken();
            if (t == null) {
                t = parser.nextToken();
                if (t == null) {
                    throw JsonMappingException.from(parser, "No content to map due to end-of-input");
                }
            }


            if (t == JsonToken.START_ARRAY) {
                result = getMapper().readValue(parser, getMapper().getTypeFactory().constructCollectionType(List.class, PublicApiResource.class));
            } else if (t == JsonToken.START_OBJECT) {
                result = getMapper().readValue(parser, PublicApiResource.ResourceHolder.class).collection;
            } else {
                throw JsonMappingException.from(parser, "Invalid content");
            }

            if (result == null) {
                result = Collections.emptyList();
            }
        } finally {
            IOUtils.close(parser);
            //Incase parser creation failed, should fail silently otherwise
            IOUtils.close(is);
        }

        return result;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PublicApiResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        List<PublicApiResource> resources = new ArrayList<>(ids.size());
        int i = 0;
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, ids.size()));
            List<PublicApiResource> res = readList(
                    new Request(request)
                            .add(LINKED_PARTITIONING, "1")
                            .add("limit", API_LOOKUP_BATCH_SIZE)
                            .add("ids", TextUtils.join(",", batch))
            );

            resources.addAll(res);
            i += API_LOOKUP_BATCH_SIZE;
        }
        return (List<T>) resources;
    }

    public
    @NotNull
    <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request,
                                                                  Class<C> ch) throws IOException {
        List<T> objects = new ArrayList<>();
        C holder = null;
        do {
            Request r = holder == null ? request : Request.to(holder.next_href);
            r = r.with(LINKED_PARTITIONING, "1");
            InputStream inputStream = getInputStream(get(r), r);
            try {
                holder = getMapper().readValue(inputStream, ch);
            } finally {
                IOUtils.close(inputStream);
            }
            if (holder == null) {
                throw new IOException("invalid data");
            }

            if (holder.collection != null) {
                objects.addAll(holder.collection);
            }
        } while (!TextUtils.isEmpty(holder.next_href));

        return objects;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PublicApiResource> PublicApiResource.ResourceHolder<T> readCollection(Request req) throws IOException {
        InputStream inputStream = getInputStream(get(req), req);
        try {
            return getMapper().readValue(inputStream, PublicApiResource.ResourceHolder.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    public Env getEnv() {
        return env;
    }

    public String getUserAgent() {
        return userAgent == null ? USER_AGENT : userAgent;
    }

    private InputStream getInputStream(HttpResponse response, Request originalRequest) throws IOException {
        final int code = response.getStatusLine().getStatusCode();
        switch (code) {
            case HttpStatus.SC_NOT_FOUND:
                throw new NotFoundException();
            default:
                if (!isStatusCodeOk(code)) {
                    throw new UnexpectedResponseException(originalRequest, response.getStatusLine());
                }
        }
        return response.getEntity().getContent();
    }

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
     * @throws java.io.IOException   network error
     * @throws InvalidTokenException unauthorized
     * @throws ApiResponseException  http error
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
                new InvalidTokenException(status, error) :
                new ApiResponseException(response, error);
    }

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
                            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, PublicApi.REALM, OAUTH_SCHEME),
                            OAuth2Scheme.EmptyCredentials.INSTANCE);

                    getAuthSchemes().register(PublicApi.OAUTH_SCHEME, new OAuth2Scheme.Factory(PublicApi.this));

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
                            Arrays.asList(PublicApi.OAUTH_SCHEME, "digest", "basic"));
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

    public HttpResponse get(Request request) throws IOException {
        return execute(request, HttpGet.class);
    }

    public HttpResponse put(Request request) throws IOException {
        return execute(request, HttpPut.class);
    }

    public HttpResponse post(Request request) throws IOException {
        return execute(request, HttpPost.class);
    }

    public HttpResponse delete(Request request) throws IOException {
        return execute(request, HttpDelete.class);
    }

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
    public HttpResponse safeExecute2TheSequel(HttpHost target, HttpUriRequest request) throws IOException {
        if (target == null) {
            target = determineTarget(request);
            String hostString;
            if (target == null) {
                hostString = "null";
            } else {
                hostString = String.format("%s://%s", target.getSchemeName(), target.toHostString());
            }
            log(INFO, TAG, String.format("automatically determine target to be %s", hostString));
            if (target == null) {
                throw new NullPointerException("Api wrapper was passed a 'null' target, and could not determine a default");
            }
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
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // IllegalArgumentException is caught because of:
            // more brokenness
            // cf. http://code.google.com/p/android/issues/detail?id=2690

            // ArrayIndexOutOfBoundsException is caught because of:
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

    public String getDefaultAcceptEncoding() {
        return defaultAcceptEncoding;
    }

    public void setDefaultAcceptEncoding(String encoding) {
        defaultAcceptEncoding = encoding;
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

    // This design existed when the library was brought in to the project. Changing
    // it does not seem worthwhile, since the reassignment is once during validation
    // of inputs.
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    protected HttpResponse execute(Request req, Class<? extends HttpRequestBase> reqType) throws IOException {
        Request defaults = defaultParams.get();
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

    public static class CloudDateFormat extends StdDateFormat {
        // SimpleDateFormat & co are not threadsafe - use thread local instance for static access
        private static ThreadLocal<CloudDateFormat> threadLocal = new ThreadLocal<>();
        /**
         * Used by the SoundCloud API
         */
        private final DateFormat dateFormat;

        private CloudDateFormat() {
            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        private static CloudDateFormat instance() {
            CloudDateFormat fmt = threadLocal.get();
            if (fmt == null) {
                fmt = new CloudDateFormat();
                threadLocal.set(fmt);
            }
            return fmt;
        }

        public static Date fromString(String s) {
            try {
                return instance().parse(s);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        public static long toTime(String s) {
            return fromString(s).getTime();
        }

        public static String formatDate(long tstamp) {
            return instance().format(tstamp);
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public CloudDateFormat clone() {
            return instance();
        }

        @Override
        public Date parse(String dateStr, ParsePosition pos) {
            final Date d = dateFormat.parse(dateStr, pos);
            return (d == null) ? super.parse(dateStr, pos) : d;
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            return dateFormat.format(date, toAppendTo, fieldPosition);
        }
    }
}
