package com.soundcloud.android;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Env;
import com.soundcloud.api.Http;
import com.soundcloud.api.Token;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.StdDateFormat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.os.Build;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface AndroidCloudAPI extends CloudAPI {
    URI REDIRECT_URI = URI.create("soundcloud://auth");

    ObjectMapper getMapper();

    public static class Wrapper extends ApiWrapper implements AndroidCloudAPI {
        private ObjectMapper mMapper;
        private Context mContext;
        private String userAgent;

        public Wrapper(Context context, String clientId, String clientSecret, URI redirectUri, Token token, Env env) {
            super(clientId, clientSecret, redirectUri, token, env);
            mContext = context;
            try {
                PackageInfo info = mContext.getPackageManager().getPackageInfo(AndroidCloudAPI.class.getPackage().getName(),
                        PackageManager.GET_META_DATA);
                userAgent = "SoundCloud Android ("+info.versionName+")";
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        @Override protected SSLSocketFactory getSSLSocketFactory() {
            return SoundCloudApplication.DALVIK && Build.VERSION.SDK_INT >= 8 ?
                    SSLCertificateSocketFactory.getHttpSocketFactory(Http.TIMEOUT, new SSLSessionCache(mContext)) :
                    super.getSSLSocketFactory();
        }

        @Override public ObjectMapper getMapper() {
            if (this.mMapper == null) {
                mMapper = createMapper();
            }
            return mMapper;
        }

        @Override protected String getUserAgent() {
            return userAgent == null ? super.getUserAgent() : userAgent;
        }

        public static ObjectMapper createMapper() {
            return new ObjectMapper() {
                {
                    getDeserializationConfig().setDateFormat(CloudDateFormat.INSTANCE);
                }
            };
        }
    }

    static class CloudDateFormat extends StdDateFormat {
        /** Used by the SoundCloud API */
        public static final DateFormat CLOUDDATEFMT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
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
    }
}
