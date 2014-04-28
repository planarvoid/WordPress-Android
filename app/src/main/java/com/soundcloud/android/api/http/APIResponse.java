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

    private final int statusCode;
    private final String responseBody;
    private final Map<String, String> responseHeaders;

    protected APIResponse(int statusCode, String responseBody, Header[] responseHeaders) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.responseHeaders = createHeadersMap(responseHeaders);
    }

    public boolean isSuccess() {
        //TODO Wondering if this should not go beyond 300 status code
        return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_BAD_REQUEST;
    }

    public boolean accountIsRateLimited() {
        return statusCode == SC_REQUEST_TOO_MANY_REQUESTS;
    }

    public boolean responseCodeisForbidden() {
        return statusCode == HttpStatus.SC_FORBIDDEN;
    }

    public boolean hasResponseBody() {
        return !isNullOrEmpty(nullToEmpty(responseBody).trim());
    }

    public String getHeader(String key) {
        return responseHeaders.get(key);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
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
                .add("statusCode", statusCode)
                .add("responseHeader", responseHeaders).toString();
    }
}
