package com.soundcloud.api;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Http {
    public static final int BUFFER_SIZE = 8192;
    public static final int TIMEOUT = 20 * 1000;

    public static String getString(HttpResponse response) throws IOException {
        InputStream is = response.getEntity().getContent();
        if (is == null) return null;

        int length = BUFFER_SIZE;
        Header contentLength = null;
        try {
            contentLength = response.getFirstHeader(HTTP.CONTENT_LEN);
        } catch (UnsupportedOperationException ignored) {
        }

        if (contentLength != null) {
            try {
                length = Integer.parseInt(contentLength.getValue());
            } catch (NumberFormatException ignored) {
            }
        }

        final StringBuilder sb = new StringBuilder(length);
        int n;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }


    /**
     * @see android.net.http.AndroidHttpClient#newInstance(String, Context)
     * @return the default HttpParams
     */
    public static HttpParams defaultParams(){
        final HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // fix contributed by Bjorn Roche (XXX check if still needed)
        params.setBooleanParameter("http.protocol.expect-continue", false);
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRoute() {
            @Override
            public int getMaxForRoute(HttpRoute httpRoute) {
                return ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE * 3;
            }
        });
        return params;
    }


    /** Convenience class for passing parameters to HTTP methods */
    public static class Params implements Iterable<NameValuePair> {
        public List<NameValuePair> params = new ArrayList<NameValuePair>();

        public Params(Object... args) {
            if (args != null) {
                if (args.length % 2 != 0) throw new IllegalArgumentException("need even number of arguments");
                for (int i = 0; i < args.length; i += 2) {
                    this.add(args[i].toString(), args[i + 1]);
                }
            }
        }

        public Params add(String name, Object value) {
            params.add(new BasicNameValuePair(name, String.valueOf(value)));
            return this;
        }

        public int size() {
            return params.size();
        }

        public String queryString() {
            return URLEncodedUtils.format(params, "UTF-8");
        }

        public String url(String url) {
            return params.isEmpty() ? url : url + "?" + queryString();
        }

        @Override
        public String toString() {
            return queryString();
        }

        @Override
        public Iterator<NameValuePair> iterator() {
            return params.iterator();
        }
    }
}
