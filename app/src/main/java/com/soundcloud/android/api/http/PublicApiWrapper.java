package com.soundcloud.android.api.http;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.preference.PreferenceManager;
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
import java.util.TimeZone;

public class PublicApiWrapper extends ApiWrapper implements PublicCloudAPI {

    /**
     * the parameter which we use to tell the API that this is a non-interactive request (e.g. background syncing.
     */
    public static final String BACKGROUND_PARAMETER = "_behavior[non_interactive]";

    public static final String LINKED_PARTITIONING = "linked_partitioning";

    private static final int API_LOOKUP_BATCH_SIZE = 200;

    private static PublicApiWrapper instance;
    private ApplicationProperties mApplicationProperties;
    private ObjectMapper mObjectMapper;
    private Context mContext;
    private String userAgent;
    private UnauthorisedRequestRegistry mUnauthorisedRequestRegistry;

    public synchronized static PublicApiWrapper getInstance(Context context) {
        if (instance == null) {
            instance = new PublicApiWrapper(context.getApplicationContext());
        }
        return instance;
    }

    @Deprecated
    public PublicApiWrapper(Context context) {
        this(context, new HttpProperties(context), new AccountOperations(context), new ApplicationProperties(context.getResources()));

    }

    @Deprecated
    protected PublicApiWrapper(Context context, HttpProperties properties, AccountOperations accountOperations,
                               ApplicationProperties applicationProperties) {
        this(context, buildObjectMapper(), properties.getClientId(), properties.getClientSecret(),
                ANDROID_REDIRECT_URI, accountOperations.getSoundCloudToken(), applicationProperties,
                UnauthorisedRequestRegistry.getInstance(context));
    }

    private PublicApiWrapper(Context context, ObjectMapper mapper, String clientId, String clientSecret, URI redirectUri,
                             Token token, ApplicationProperties applicationProperties, UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        super(clientId, clientSecret, redirectUri, token);
        // context can be null in tests
        if (context == null) return;
        mUnauthorisedRequestRegistry = unauthorisedRequestRegistry;
        mApplicationProperties = applicationProperties;
        mContext = context;
        mObjectMapper = mapper;
        setTokenListener(new SoundCloudTokenListener(context));
        userAgent = "SoundCloud Android (" + AndroidUtils.getAppVersion(context, "unknown") + ")";
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


    public final static ObjectMapper buildObjectMapper() {
        return new ObjectMapper().
                configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
                setDateFormat(new CloudDateFormat());
    }

    @Override
    public void setProxy(URI proxy) {
        super.setProxy(proxy);

        if (mApplicationProperties.shouldEnableNetworkProxy()) {
            getHttpClient().getConnectionManager().getSchemeRegistry()
                    .register(new Scheme("https",
                            proxy != null ? unsafeSocketFactory() : getSSLSocketFactory(), 443));
        }

    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    @Override
    protected SSLSocketFactory getSSLSocketFactory() {
        //Why do we do this differentiation? Why not just use the standard one?
        if (mApplicationProperties.isRunningOnDalvik()) {
            // make use of android's implementation
            return SSLCertificateSocketFactory.getHttpSocketFactory(ApiWrapper.TIMEOUT,
                    new SSLSessionCache(mContext));
        } else {
            // httpclient default
            return super.getSSLSocketFactory();
        }
    }

    @Override
    public ObjectMapper getMapper() {
        return mObjectMapper;
    }

    // add a bunch of logging in debug mode to make it easier to see and debug API request
    @Override
    public HttpResponse safeExecute(HttpHost target, HttpUriRequest request) throws IOException {
        // sends the request
        HttpResponse response = null;
        try {
            response = super.safeExecute(target, request);
            recordUnauthorisedRequestIfRequired(response);
        } finally {
            logRequest(request, response);
        }

        return response;
    }

    private void recordUnauthorisedRequestIfRequired(HttpResponse response) {
        if (responseIsUnauthorised(response)) {
            mUnauthorisedRequestRegistry.updateObservedUnauthorisedRequestTimestamp();
        }
    }

    private void logRequest(HttpUriRequest request, @Nullable HttpResponse response) {
        if (!mApplicationProperties.isReleaseBuild()) {
            String report = generateRequestResponseLog(request, response);
            // we log using INFO level, since request logs can be useful in beta builds
            Log.i(TAG, report);
        }
    }

    private boolean responseIsUnauthorised(HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    public static String generateRequestResponseLog(HttpUriRequest request, @Nullable HttpResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ").append(request.getURI());
        sb.append(";headers=");
        final Header[] headers = request.getAllHeaders();
        for (Header header : headers) {
            sb.append(header.toString()).append(";");
        }
        sb.append("response=").append(response == null ? "NULL" : response.getStatusLine());
        return sb.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScResource> T read(Request request) throws NotFoundException, IOException {
        InputStream inputStream = getInputStream(get(request), request);
        try {
            return (T) getMapper().readValue(inputStream, ScResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        InputStream inputStream = getInputStream(put(request), request);
        try {
            return (T) getMapper().readValue(inputStream, ScResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScResource> T create(Request request) throws IOException {
        InputStream inputStream = getInputStream(post(request), request);
        try {
            return (T) getMapper().readValue(inputStream, ScResource.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends ScResource> List<T> readList(Request request) throws IOException {
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
                result = getMapper().readValue(parser, getMapper().getTypeFactory().constructCollectionType(List.class, ScResource.class));
            } else if (t == JsonToken.START_OBJECT) {
                result = getMapper().readValue(parser, ScResource.ScResourceHolder.class).collection;
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

    @SuppressWarnings("unchecked")
    public <T extends ScResource> List<T> readListFromIds(Request request, List<Long> ids) throws IOException {
        List<ScResource> resources = new ArrayList<ScResource>(ids.size());
        int i = 0;
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, ids.size()));
            List<ScResource> res = readList(
                    new Request(request)
                            .add(LINKED_PARTITIONING, "1")
                            .add("limit", API_LOOKUP_BATCH_SIZE)
                            .add("ids", TextUtils.join(",", batch)));

            resources.addAll(res);
            i += API_LOOKUP_BATCH_SIZE;
        }
        return (List<T>) resources;
    }

    @Override
    public
    @NotNull
    <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request,
                                                                  Class<C> ch) throws IOException {
        List<T> objects = new ArrayList<T>();
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
            if (holder == null) throw new IOException("invalid data");

            if (holder.collection != null) {
                objects.addAll(holder.collection);
            }
        } while (!TextUtils.isEmpty(holder.next_href));

        return objects;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        InputStream inputStream = getInputStream(get(req), req);
        try{
            return getMapper().readValue(inputStream, ScResource.ScResourceHolder.class);
        } finally {
            IOUtils.close(inputStream);
        }
    }

    @Override
    public Env getEnv() {
        return env;
    }

    @Override
    public String getUserAgent() {
        return userAgent == null ? super.getUserAgent() : userAgent;
    }

    /**
     * @param enabled if true, all requests will be tagged as coming from a background process
     */
    public static void setBackgroundMode(boolean enabled) {
        if (enabled) {
            ApiWrapper.setDefaultParameter(BACKGROUND_PARAMETER, "1");
        } else {
            ApiWrapper.clearDefaultParameters();
        }
    }

    private InputStream getInputStream(HttpResponse response, Request originalRequest) throws IOException {
        final int code = response.getStatusLine().getStatusCode();
        switch (code) {
            case HttpStatus.SC_NOT_FOUND:
                throw new NotFoundException();
            default:
                if (!isStatusCodeOk(code)) {
                    final UnexpectedResponseException exception = new UnexpectedResponseException(originalRequest, response.getStatusLine());
                    throw exception;
                }
        }
        return response.getEntity().getContent();
    }


    public static boolean isStatusCodeOk(int code) {
        return code >= 200 && code < 400;
    }

    public static boolean isStatusCodeError(int code) {
        return code >= 400 && code < 600;
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

    public static class CloudDateFormat extends StdDateFormat {
        /**
         * Used by the SoundCloud API
         */
        private final DateFormat dateFormat;
        // SimpleDateFormat & co are not threadsafe - use thread local instance for static access
        private static ThreadLocal<CloudDateFormat> threadLocal = new ThreadLocal<CloudDateFormat>();

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

        private CloudDateFormat() {
            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
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

    public static class UnauthorizedException extends InvalidTokenException {

        public UnauthorizedException(String requestLog, @Nullable Token token) {
            super(401, requestLog + "; token=" + token);
        }
    }
}
