package com.soundcloud.utils.http;

import com.soundcloud.android.CloudUtils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Http {
    public static class Params {
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


        public String queryString() {
            return URLEncodedUtils.format(params, "UTF-8");
        }

        @Override
        public String toString() {
            return queryString();
        }
    }

    public static interface ProgressListener {
        public void transferred(long amount);
    }

    // perform a request *without* following redirects
    // useful for checking status codes like 302/303
    public static HttpResponse noRedirect(HttpUriRequest req) throws IOException {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        DefaultHttpClient client = new DefaultHttpClient(CloudUtils.getConfiguredDefaultHttpParams());
        client.setRedirectHandler(new RedirectHandler() {
            @Override
            public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                return false;
            }

            @Override
            public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
                return null;
            }
        });
        return client.execute(req);
    }
}
