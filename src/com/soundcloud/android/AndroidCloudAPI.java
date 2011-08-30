package com.soundcloud.android;

import com.soundcloud.android.json.EventDeserializer;
import com.soundcloud.android.json.EventSerializer;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.util.StdDateFormat;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;

import java.net.URI;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public interface AndroidCloudAPI extends CloudAPI {
    public static final ObjectMapper Mapper = Wrapper.Mapper;
    URI REDIRECT_URI = URI.create("soundcloud://auth");
    String OAUTH_TOKEN_PARAMETER = "oauth_token";

    ObjectMapper getMapper();
    String addTokenToUrl(String url);

    public static class Wrapper extends ApiWrapper implements AndroidCloudAPI {
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
            }
        }

        @Override protected SSLSocketFactory getSSLSocketFactory() {
            return SoundCloudApplication.DALVIK && Build.VERSION.SDK_INT >= 8 ?
                    SSLCertificateSocketFactory.getHttpSocketFactory(ApiWrapper.TIMEOUT, new SSLSessionCache(mContext)) :
                    super.getSSLSocketFactory();
        }

        @Override public ObjectMapper getMapper() {
            return Mapper;
        }

        @Override protected String getUserAgent() {
            return userAgent == null ? super.getUserAgent() : userAgent;
        }

        @Override
        public String addTokenToUrl(String url) {
            if (getToken().valid()) {
                return Request.to(url).with(OAUTH_TOKEN_PARAMETER, getToken().access).toUrl();
            } else {
                return url;
            }
        }

        public static ObjectMapper createMapper() {
            return new ObjectMapper() {
                {
                    configure(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false);
                    setDateFormat(CloudDateFormat.INSTANCE);
                }
            };
        }
    }

    static class CloudDateFormat extends StdDateFormat {
        /** Used by the SoundCloud API */
        public static final DateFormat CLOUDDATEFMT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");

        static {
            CLOUDDATEFMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        public static final DateFormat INSTANCE = new CloudDateFormat();

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
