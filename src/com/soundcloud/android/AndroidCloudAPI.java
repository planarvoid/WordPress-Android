package com.soundcloud.android;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Token;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.StdDateFormat;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface AndroidCloudAPI extends CloudAPI {
    String REDIRECT_URI = "soundcloud://auth";

    ObjectMapper getMapper();

    public static class Wrapper extends ApiWrapper implements AndroidCloudAPI {
        private ObjectMapper mMapper;

        public Wrapper(String clientId, String clientSecret, String redirectUri, Token token, Env env) {
            super(clientId, clientSecret, redirectUri, token, env);
        }

        // XXX reenable when sandbox certificate is working again
        /*
        @Override protected SSLSocketFactory getSSLSocketFactory() {
            return isRunningOnDalvik() ?
                    SSLCertificateSocketFactory.getHttpSocketFactory(Http.TIMEOUT, null) :
                    super.getSSLSocketFactory();
        }
        */

        @Override public ObjectMapper getMapper() {
            if (this.mMapper == null) {
                mMapper = createMapper();
            }
            return mMapper;
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
