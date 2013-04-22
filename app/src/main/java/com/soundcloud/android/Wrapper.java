package com.soundcloud.android;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpRequestBase;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Wrapper extends ApiWrapper implements AndroidCloudAPI {
    /**
     * the parameter which we use to tell the API that this is a non-interactive request (e.g. background syncing.
     */
    public static final String BACKGROUND_PARAMETER = "_behavior[non_interactive]";

    public static final String LINKED_PARTITIONING = "linked_partitioning";

    private static final int API_LOOKUP_BATCH_SIZE = 200;

    private ObjectMapper mObjectMapper;
    private Context mContext;
    private String userAgent;

    public Wrapper(Context context, @Nullable Token initialToken) {
        this(context, buildObjectMapper(), context.getString(R.string.client_id), getClientSecret(true), ANDROID_REDIRECT_URI, initialToken);
    }

    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper().
                configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                configure(SerializationFeature.WRAP_ROOT_VALUE, true).
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
                setDateFormat(new CloudDateFormat());
    }


    private Wrapper(Context context, ObjectMapper mapper, String clientId, String clientSecret, URI redirectUri, Token token) {
        super(clientId, clientSecret, redirectUri, token);
        // context can be null in tests
        if (context == null) return;

        mContext = context;
        mObjectMapper = mapper;

        userAgent = "SoundCloud Android ("+ AndroidUtils.getAppVersion(context, "unknown")+")";
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Actions.CHANGE_PROXY_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                final String proxy = intent.getStringExtra(Actions.EXTRA_PROXY);
                Log.d(TAG, "proxy changed: " + proxy);
                setProxy(proxy == null ? null : URI.create(proxy));
            }
        }, filter);

        if (SoundCloudApplication.DEV_MODE) {
            final String proxy =
                    PreferenceManager.getDefaultSharedPreferences(context).getString(Consts.PrefKeys.DEV_HTTP_PROXY, null);
            setProxy(TextUtils.isEmpty(proxy) ? null : URI.create(proxy));
        }

        setDefaultAcceptEncoding("gzip");
    }


    @Override
    protected void logRequest(Class<? extends HttpRequestBase> reqType, Request request) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, reqType.getSimpleName()+" "+request);
        }
    }

    @Override
    public void setProxy(URI proxy) {
        super.setProxy(proxy);

        if (SoundCloudApplication.DEV_MODE) {
            getHttpClient().getConnectionManager().getSchemeRegistry()
                           .register(new Scheme("https",
                                   proxy != null ? unsafeSocketFactory() : getSSLSocketFactory(), 443));
        }

    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    @Override protected SSLSocketFactory getSSLSocketFactory() {
        if (SoundCloudApplication.DALVIK &&
            env == Env.LIVE) {
            // make use of android's implementation
            return SSLCertificateSocketFactory.getHttpSocketFactory(ApiWrapper.TIMEOUT,
                    new SSLSessionCache(mContext));
        } else {
            // httpclient default
            return super.getSSLSocketFactory();
        }
    }

    @Override public ObjectMapper getMapper() {
        return mObjectMapper;
    }

    @Override
    public Context getContext() {
        return  mContext;
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends ScResource> T read(Request request) throws NotFoundException, IOException {
        return (T) getMapper().readValue(getInputStream(get(request)), ScResource.class);
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends ScResource> T update(Request request) throws NotFoundException, IOException {
        return (T) getMapper().readValue(getInputStream(put(request)), ScResource.class);
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends ScResource> T create(Request request) throws IOException {
        return (T) getMapper().readValue(getInputStream(post(request)), ScResource.class);
    }

    @Override @SuppressWarnings("unchecked")
    public <T extends ScResource> List<T> readList(Request request) throws IOException {
        InputStream is = getInputStream(get(request));

        JsonParser parser = getMapper().getFactory().createParser(is);
        JsonToken t = parser.getCurrentToken();
        if (t == null) {
            t = parser.nextToken();
            if (t == null) {
                throw JsonMappingException.from(parser, "No content to map due to end-of-input");
            }
        }
        try {
            if (t == JsonToken.START_ARRAY) {
                return getMapper().readValue(parser, getMapper().getTypeFactory().constructCollectionType(List.class, ScResource.class));
            }  else if (t == JsonToken.START_OBJECT) {
                return getMapper().readValue(parser, ScResource.ScResourceHolder.class).collection;
            } else {
                throw JsonMappingException.from(parser, "Invalid content");
            }
        } finally {
             parser.close();
        }
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
    public @NotNull <T, C extends CollectionHolder<T>> List<T> readFullCollection(Request request,
                                                                                  Class<C> ch) throws IOException {
        List<T> objects = new ArrayList<T>();
        C holder = null;
        do {
            Request r = holder == null ? request : Request.to(holder.next_href);
            holder = getMapper().readValue(getInputStream(get(r.with(LINKED_PARTITIONING, "1"))), ch);
            if (holder == null) throw new IOException("invalid data");

            if (holder.collection != null) {
                objects.addAll(holder.collection);
            }
        } while (!TextUtils.isEmpty(holder.next_href));

        return objects;
    }


    @Override @SuppressWarnings("unchecked")
    public <T extends ScResource> ScResource.ScResourceHolder<T> readCollection(Request req) throws IOException {
        return getMapper().readValue(getInputStream(get(req)), ScResource.ScResourceHolder.class);
    }

    @Override
    public Env getEnv() {
        return env;
    }

    @Override public String getUserAgent() {
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

    private InputStream getInputStream(HttpResponse response) throws IOException, NotFoundException {
        final int code = response.getStatusLine().getStatusCode();
        switch (code) {
            case HttpStatus.SC_UNAUTHORIZED:
                throw new InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                        response.getStatusLine().getReasonPhrase());

            case HttpStatus.SC_NOT_FOUND:
                throw new NotFoundException();
            default:
                if (!isStatusCodeOk(code)) {
                    throw new IOException("Invalid response: " + response.getStatusLine());
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

    public static boolean isStatusCodeServerError(int code) {
        return code >= 500 && code < 600;
    }

    /* package */ static String getClientSecret(boolean production) {
        @SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray"})
        final long[] prod =
                new long[]{0x42D31224F5C2C264L, 0x5986B01A2300AFA4L, 0xEDA169985C1BA18DL,
                        0xA2A0313C7077F81BL, 0xF42A7E5EEB220859L, 0xE593789593AFFA3L,
                        0xF564A09AA0B465A6L};

        final long[] prod2 =
                new long[]{0xCFDBF8AB10DCADA3L, 0x6C580A13A4B7801L, 0x607547EC749EBFB4L,
                        0x300C455E649B39A7L, 0x20A6BAC9576286CBL};

        final long[] sandbox =
                new long[]{0x7FA4855507D9000FL, 0x91C67776A3692339L, 0x24D0C4EF5AF943E8L,
                        0x7CEC0CF7DDAAE26BL, 0x7EB2854D631380BEL};

        return ScTextUtils.deobfuscate(production ? prod2 : sandbox);
    }

    // accepts all certificates - don't use in production
    private static SSLSocketFactory unsafeSocketFactory() {
        try {
            final SSLContext context = SSLContext.getInstance("TLS");
            return new SSLSocketFactory(null, null, null, null, null, null) {
                {
                    context.init(null, new TrustManager[] { new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
                            Log.w(TAG, "trusting "+ Arrays.asList(chain));
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
                            Log.w(TAG, "trusting "+ Arrays.asList(chain));
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
        /** Used by the SoundCloud API */
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
}
