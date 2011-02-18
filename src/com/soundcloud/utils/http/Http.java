package com.soundcloud.utils.http;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

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
}
