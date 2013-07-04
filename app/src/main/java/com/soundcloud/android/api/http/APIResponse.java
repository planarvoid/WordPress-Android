package com.soundcloud.android.api.http;


import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.uniqueIndex;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.apache.http.Header;
import org.apache.http.HttpStatus;

import javax.annotation.Nullable;
import java.util.Map;

public class APIResponse {
    private static final int SC_REQUEST_TOO_MANY_REQUESTS = 429;

    private static final Function<Header, String> HEADER_KEY_FUNCTION = new Function<Header, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Header input) {
            return input == null ? null : input.getName();
        }
    };
    private static final Function<Header, String> HEADER_VALUE_FUNCTION = new Function<Header, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Header input) {
            return input == null ? null : input.getValue();
        }
    };

    private final int mStatusCode;
    private final String mResponseBody;
    private final Map<String, String> mResponseHeaders;

    protected APIResponse(int statusCode, String responseBody, Header[] responseHeaders) {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = createHeadersMap(responseHeaders);
    }

    public boolean isSuccess() {
        //TODO Wondering if this should not go beyond 300 status code
        return mStatusCode >= HttpStatus.SC_OK && mStatusCode < HttpStatus.SC_BAD_REQUEST;
    }

    public boolean accountIsRateLimited() {
        return mStatusCode == SC_REQUEST_TOO_MANY_REQUESTS;
    }

    public boolean responseCodeisForbidden() {
        return mStatusCode == HttpStatus.SC_FORBIDDEN;
    }

    public boolean hasResponseBody() {
        return !isNullOrEmpty(nullToEmpty(mResponseBody).trim());
    }

    public String getHeader(String key) {
        return mResponseHeaders.get(key);
    }

    public String getResponseBody() {
        return mResponseBody;
    }

    public boolean isNotSuccess() {
        return !isSuccess();
    }

    private Map<String, String> createHeadersMap(Header[] allHeaders) {
        return Maps.transformValues(uniqueIndex(newArrayList(allHeaders), HEADER_KEY_FUNCTION), HEADER_VALUE_FUNCTION);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("statusCode", mStatusCode)
                .add("responseHeader", mResponseHeaders).toString();
    }
}
