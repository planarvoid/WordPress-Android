package com.soundcloud.api;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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


    public static interface ProgressListener {
        public void transferred(long amount);
    }

    /** Convenience class for passing parameters to HTTP methods */
    public static class Params implements Iterable<NameValuePair> {
        Token token;
        Map<String,File> files;
        public List<NameValuePair> params = new ArrayList<NameValuePair>();
        private ProgressListener listener;

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

        public Params withToken(Token t) {
            token = t;
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

        public Params addFile(String name, File file) {
            if (files == null) files = new HashMap<String,File>();
            if (file != null)  files.put(name, file);
            return this;
        }

        public Params setProgressListener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }

        public HttpRequest buildRequest(Class<? extends HttpRequestBase> method, String resource) {
            try {
                HttpRequestBase request = method.newInstance();
                if (token != null) {
                    request.addHeader(ApiWrapper.getOAuthHeader(token));
                }

                if (files != null && !files.isEmpty() && request instanceof HttpEntityEnclosingRequestBase) {
                    MultipartEntity entity = new MultipartEntity();
                    for (Map.Entry<String,File> e : files.entrySet()) {
                        entity.addPart(e.getKey(), new FileBody(e.getValue()));
                    }
                    for (NameValuePair pair : params) {
                        try {
                            entity.addPart(pair.getName(), new StringBodyNoHeaders(pair.getValue()));
                        } catch (UnsupportedEncodingException ignored) {
                        }
                    }

                    ((HttpEntityEnclosingRequestBase)request).setEntity(listener == null ? entity :
                        new CountingMultipartRequestEntity(entity, listener));

                    request.setURI(URI.create(resource));
                } else {
                    request.setURI(URI.create(url(resource)));
                }
                return request;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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

      public static class StringBodyNoHeaders extends StringBody {
        public StringBodyNoHeaders(String value) throws UnsupportedEncodingException {
            super(value);
        }

        @Override public String getMimeType() {
            return null;
        }

        @Override public String getTransferEncoding() {
            return null;
        }
    }
}
