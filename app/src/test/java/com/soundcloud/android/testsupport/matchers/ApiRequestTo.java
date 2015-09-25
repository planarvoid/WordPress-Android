package com.soundcloud.android.testsupport.matchers;

import com.soundcloud.android.api.ApiMultipartRequest;
import com.soundcloud.android.api.ApiObjectContentRequest;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.FormPart;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.objects.MoreObjects;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiRequestTo extends ArgumentMatcher<ApiRequest> {

    private final boolean isMobileApi;
    private Map<String, String> expectedQueryParams = new HashMap<>();
    private Map<String, String> expectedHeaders = new HashMap<>();
    private List<String> unExpectedHeaders = new ArrayList<>();
    private boolean queryMatchError, headerMatchError, formMatchError;
    private String expectedMethod, expectedPath;
    private ApiRequest request;
    private Object content;
    private List<FormPart> formParts;

    public ApiRequestTo(String expectedMethod, String expectedPath, boolean isMobileApi) {
        this.expectedMethod = expectedMethod;
        this.expectedPath = expectedPath;
        this.isMobileApi = isMobileApi;
    }

    @Override
    public boolean matches(Object argument) {
        if (argument instanceof ApiRequest) {
            this.request = (ApiRequest) argument;

            return containsExpectedQueryParams()
                    && containsExpectedHeaders()
                    && contentMatches()
                    && formPartsMatch()
                    && pathMatches()
                    && methodMatches()
                    && request.isPrivate() == isMobileApi;
        }
        return false;
    }

    private boolean containsExpectedHeaders() {
        for (Map.Entry<String, String> param : expectedHeaders.entrySet()) {
            boolean headersMatch = request.getHeaders().get(param.getKey()).equals(param.getValue());
            if (!headersMatch) {
                headerMatchError = true;
                return false;
            }
        }

        for (String header : unExpectedHeaders) {
            boolean headersMatch = !request.getHeaders().containsKey(header);
            if (!headersMatch) {
                headerMatchError = true;
                return false;
            }
        }
        return true;
    }

    private boolean pathMatches() {
        return expectedPath == null || request.getEncodedPath().equals(expectedPath);
    }

    private boolean methodMatches() {
        return expectedMethod == null || request.getMethod().equalsIgnoreCase(expectedMethod);
    }

    private boolean containsExpectedQueryParams() {
        for (Map.Entry<String, String> param : expectedQueryParams.entrySet()) {
            boolean queryMatches = request.getQueryParameters().get(param.getKey()).contains(param.getValue());
            if (!queryMatches) {
                queryMatchError = true;
                return false;
            }
        }
        return true;
    }

    private boolean contentMatches() {
        if (request instanceof ApiObjectContentRequest) {
            Object targetContent = ((ApiObjectContentRequest) request).getContent();
            if (content instanceof Iterable) {
                return targetContent instanceof Iterable
                        && Iterables.elementsEqual((Iterable) content, (Iterable) targetContent);
            } else if (content instanceof Map && targetContent instanceof Map) {
                return Iterables.elementsEqual(((Map) content).entrySet(), ((Map) targetContent).entrySet());
            } else {
                return MoreObjects.equal(targetContent, content);
            }
        } else {
            // must not expect a content to exist if target request not a content request
            return content == null;
        }
    }

    private boolean formPartsMatch() {
        if (formParts != null && request instanceof ApiMultipartRequest) {
            final List<FormPart> matchedParts = ((ApiMultipartRequest) request).getParts();
            // don't check with equals, as that assumes order, which isn't required here.
            final boolean matches = formParts.containsAll(matchedParts) && matchedParts.containsAll(formParts);
            if (!matches) {
                formMatchError = true;
            }
            return matches;
        }
        return true;
    }

    public ApiRequestTo withQueryParam(String key, String... values) {
        for (String value : values) {
            expectedQueryParams.put(key, value);
        }
        return this;
    }

    public ApiRequestTo withHeader(String key, String value) {
        expectedHeaders.put(key, value);
        return this;
    }


    public ApiRequestTo withoutHeader(String header) {
        unExpectedHeaders.add(header);
        return this;
    }

    public ApiRequestTo withContent(Object content){
        this.content = content;
        return this;
    }

    public ApiRequestTo withFormParts(FormPart... formParts) {
        this.formParts = Arrays.asList(formParts);
        return this;
    }

    @Override
    public void describeTo(Description description) {
        if (queryMatchError) {
            description.appendText("Expected query params to contain ");
            description.appendValue(expectedQueryParams);
            description.appendText("\nBut found ");
            description.appendValue(request.getQueryParameters());
        } else if (headerMatchError) {
            description.appendText("Expected headers to contain ");
            description.appendValue(expectedHeaders);
            description.appendText("\nBut found ");
            description.appendValue(request.getHeaders());
        } else if (formMatchError) {
            description.appendText("Multipart form parts to equal ");
            description.appendValue(formParts);
            description.appendText("\nBut found ");
            description.appendValue(((ApiMultipartRequest) request).getParts());
        } else {
            super.describeTo(description);
        }
    }
}
