package com.soundcloud.android;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.soundcloud.android.json.ActivityDeserializer;
import com.soundcloud.android.json.UserDeserializer;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
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
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

public interface AndroidCloudAPI extends CloudAPI {
    public static final ObjectMapper Mapper = Wrapper.Mapper;
    URI REDIRECT_URI = URI.create("soundcloud://auth");

    String getUserAgent();
    ObjectMapper getMapper();

    @SuppressLint("NewApi")
    public static class Wrapper extends ApiWrapper implements AndroidCloudAPI {
        public static final ObjectMapper Mapper;

        static {
            Mapper = createMapper();
            // need to create separate mapper (for user deserialization)
            final ObjectMapper mapper = createMapper();

            Mapper.registerModule(new SimpleModule("CustomDeserialization", new Version(1, 0, 0, null, null, null))
                    .addDeserializer(User.class, new UserDeserializer(mapper))
                    .addDeserializer(Activity.class, new ActivityDeserializer(Mapper)));
        }

        private Context mContext;
        private String userAgent;

        public Wrapper(Context context, String clientId, String clientSecret, URI redirectUri, Token token, Env env) {
            super(clientId, clientSecret, redirectUri, token, env);
            if (context != null) {
                mContext = context;
                userAgent = "SoundCloud Android ("+CloudUtils.getAppVersion(context, "unknown")+")";
                final IntentFilter filter = new IntentFilter();
                filter.addAction(Actions.CHANGE_PROXY_ACTION);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context context, Intent intent) {
                        final String proxy = intent.getStringExtra(Actions.EXTRA_PROXY);
                        Log.d(TAG, "proxy changed: "+proxy);
                        setProxy(proxy == null ? null : URI.create(proxy));
                    }
                }, filter);

                if (SoundCloudApplication.DEV_MODE) {
                    final String proxy =
                            PreferenceManager.getDefaultSharedPreferences(context).getString(Consts.PrefKeys.DEV_HTTP_PROXY, null);
                    setProxy(TextUtils.isEmpty(proxy) ? null : URI.create(proxy));
                }
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
                env == Env.LIVE &&
                Build.VERSION.SDK_INT >= 8) {
                // make use of android's implementation
                return SSLCertificateSocketFactory.getHttpSocketFactory(ApiWrapper.TIMEOUT,
                        new SSLSessionCache(mContext));
            } else {
                // httpclient default
                return super.getSSLSocketFactory();
            }
        }

        @Override public ObjectMapper getMapper() {
            return Mapper;
        }

        @Override public String getUserAgent() {
            return userAgent == null ? super.getUserAgent() : userAgent;
        }

        public static ObjectMapper createMapper() {
            return new ObjectMapper() {
                {
                    configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    setDateFormat(new CloudDateFormat());
                }
            };
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
            dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
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
