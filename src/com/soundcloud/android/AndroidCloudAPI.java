package com.soundcloud.android;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.json.EventDeserializer;
import com.soundcloud.android.json.EventSerializer;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.util.StdDateFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
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

    public static class Wrapper extends ApiWrapper implements AndroidCloudAPI {
        public static final String CHANGE_PROXY_ACTION = "com.soundcloud.android.CHANGE_PROXY";

        public static final ObjectMapper Mapper = createMapper();
        static {
            Mapper.registerModule(new SimpleModule("EventSupport", new Version(1, 0, 0, null))
                .addDeserializer(Event.class, new EventDeserializer())
                .addSerializer(Event.class, new EventSerializer()));
        }

        private Context mContext;
        private String userAgent;

        public Wrapper(Context context, String clientId, String clientSecret, URI redirectUri, Token token, Env env) {
            super(clientId, clientSecret, redirectUri, token, env);
            if (context != null) {
                mContext = context;
                userAgent = "SoundCloud Android ("+CloudUtils.getAppVersion(context, "unknown")+")";
                final IntentFilter filter = new IntentFilter();
                filter.addAction(CHANGE_PROXY_ACTION);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context context, Intent intent) {
                        String proxy = intent.getStringExtra("proxy");
                        setProxy(proxy == null ? null : URI.create(proxy));
                    }
                }, filter);

                if (SoundCloudApplication.DEV_MODE) {
                    final String proxy =
                            PreferenceManager.getDefaultSharedPreferences(context).getString("dev.http.proxy", null);
                    setProxy(TextUtils.isEmpty(proxy) ? null : URI.create(proxy));
                }
            }
        }

        @Override
        public void setProxy(URI proxy) {
            super.setProxy(proxy);

            getHttpClient().getConnectionManager().getSchemeRegistry()
                           .register(new Scheme("https",
                               proxy != null ? unsafeSocketFactory() : getSSLSocketFactory(), 443));

        }

        @Override protected SSLSocketFactory getSSLSocketFactory() {
            if (SoundCloudApplication.DALVIK &&
                SoundCloudApplication.API_PRODUCTION &&
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
                    configure(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false);
                    setDateFormat(CloudDateFormat.INSTANCE);
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
        public static final DateFormat CLOUDDATEFMT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");

        static {
            CLOUDDATEFMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        public static final DateFormat INSTANCE = new CloudDateFormat();

        public static Date fromString(String s) {
            try {
                return INSTANCE.parse(s);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        private CloudDateFormat() {}

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public StdDateFormat clone() {
            return new CloudDateFormat();
        }

        @Override
        public Date parse(String dateStr, ParsePosition pos) {
            final Date d = CLOUDDATEFMT.parse(dateStr, pos);
            return (d == null) ? super.parse(dateStr, pos) : d;
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            return CLOUDDATEFMT.format(date, toAppendTo, fieldPosition);
        }
    }
}
