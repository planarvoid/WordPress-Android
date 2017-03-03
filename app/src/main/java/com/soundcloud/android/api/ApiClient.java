package com.soundcloud.android.api;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.net.HttpHeaders;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.java.strings.Charsets;
import com.soundcloud.java.strings.Strings;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.os.Looper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private static final String TAG = "ApiClient";

    private final OkHttpClient httpClient;
    private final ApiUrlBuilder urlBuilder;
    private final JsonTransformer jsonTransformer;
    private final DeviceHelper deviceHelper;
    private final AdIdHelper adIdHelper;
    private final OAuth oAuth;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private final AccountOperations accountOperations;
    private final LocaleFormatter localeFormatter;
    private final boolean failFastOnMapper;

    private boolean assertBackgroundThread;


    public ApiClient(OkHttpClient httpClient, ApiUrlBuilder urlBuilder,
                     JsonTransformer jsonTransformer, DeviceHelper deviceHelper, AdIdHelper adIdHelper,
                     OAuth oAuth, UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                     AccountOperations accountOperations, LocaleFormatter localeFormatter, boolean failFastOnMapper) {
        this.httpClient = httpClient;
        this.urlBuilder = urlBuilder;
        this.jsonTransformer = jsonTransformer;
        this.deviceHelper = deviceHelper;
        this.adIdHelper = adIdHelper;
        this.oAuth = oAuth;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.accountOperations = accountOperations;
        this.localeFormatter = localeFormatter;
        this.failFastOnMapper = failFastOnMapper;
    }

    public void setAssertBackgroundThread(boolean assertBackgroundThread) {
        this.assertBackgroundThread = assertBackgroundThread;
    }

    public ApiResponse fetchResponse(ApiRequest request) {
        if (assertBackgroundThread) {
            checkState(Thread.currentThread() != Looper.getMainLooper().getThread(),
                       "Detected execution of API request on main thread");
        }
        try {
            final Request.Builder builder = new Request.Builder();

            final MultiMap<String, String> existingParams = request.getQueryParameters();
            builder.url(urlBuilder.from(request).withQueryParams(existingParams).build());
            setHttpHeaders(request, builder);

            switch (request.getMethod()) {
                case ApiRequest.HTTP_GET:
                    builder.get();
                    break;
                case ApiRequest.HTTP_POST:
                    builder.post(getRequestBody(request));
                    break;
                case ApiRequest.HTTP_PUT:
                    builder.put(getRequestBody(request));
                    break;
                case ApiRequest.HTTP_DELETE:
                    builder.delete();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
            }
            final Request httpRequest = builder.build();
            logRequest(httpRequest);
            final Response response = httpClient.newCall(httpRequest).execute();
            if (response.code() == HttpStatus.UNAUTHORIZED) {
                if (accountOperations.hasValidToken()) {
                    unauthorisedRequestRegistry.updateObservedUnauthorisedRequestTimestamp();
                }
            }
            logResponse(response);
            return new ApiResponse(request, response.code(), response.body().string());
        } catch (IOException e) {
            return new ApiResponse(ApiRequestException.networkError(request, e));
        } catch (ApiMapperException e) {
            if (failFastOnMapper) {
                throw new RuntimeException(e);
            }
            return new ApiResponse(ApiRequestException.malformedInput(request, e));
        }
    }

    private void logRequest(Request request) {
        Log.d(TAG, "[Req][" + Thread.currentThread().getName() + "] " + request.method() + " "
                + request.url().toString() + "; headers = " + request.headers().toString().replaceAll("\\n", " | "));
    }

    private void logResponse(Response response) {
        Log.d(TAG, "[Rsp][" + Thread.currentThread().getName() + "] " + response);
    }

    private void setHttpHeaders(ApiRequest request, Request.Builder builder) {
        // default headers
        builder.header(HttpHeaders.ACCEPT, request.getAcceptMediaType());
        builder.header(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
        builder.header(ApiHeaders.APP_VERSION, String.valueOf(BuildConfig.VERSION_CODE));
        builder.header(ApiHeaders.APP_ENVIRONMENT, getAppEnvironment());
        final Optional<String> locale = localeFormatter.getLocale();
        if (locale.isPresent()) {
            builder.header(ApiHeaders.DEVICE_LOCALE, locale.get());
        }

        // user identifiers
        if (accountOperations.getSoundCloudToken().valid()) {
            builder.header(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue());
        }
        builder.header(ApiHeaders.UDID, deviceHelper.getUdid());
        final Optional<String> maybeAdId = adIdHelper.getAdId();
        if (maybeAdId.isPresent()) {
            builder.header(ApiHeaders.ADID, maybeAdId.get());
            builder.header(ApiHeaders.ADID_TRACKING, String.valueOf(adIdHelper.getAdIdTracking()));
        }

        // transfer other HTTP headers
        final Map<String, String> headers = request.getHeaders();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
    }

    private String getAppEnvironment() {
        final String environment = String.valueOf(BuildConfig.APP_ENVIRONMENT);
        if (!environment.matches("(dev|alpha|beta|prod)")) {
            throw new IllegalStateException("App-Environment is not one of dev, alpha, beta or prod");
        }
        return environment;
    }

    private RequestBody getRequestBody(ApiRequest request) throws ApiMapperException, UnsupportedEncodingException {
        if (request instanceof ApiObjectContentRequest) {
            return getJsonRequestBody((ApiObjectContentRequest) request);
        } else if (request instanceof ApiMultipartRequest) {
            return getMultipartRequestBody((ApiMultipartRequest) request);
        } else {
            return RequestBody.create(MediaType.parse(request.getAcceptMediaType()), Strings.EMPTY);
        }
    }

    private RequestBody getJsonRequestBody(ApiObjectContentRequest request) throws UnsupportedEncodingException, ApiMapperException {
        final MediaType mediaType = MediaType.parse(request.getContentType());
        final byte[] content = jsonTransformer.toJson(request.getContent()).getBytes(Charsets.UTF_8.name());
        return RequestBody.create(mediaType, content);
    }

    private RequestBody getMultipartRequestBody(ApiMultipartRequest request) {
        final MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        final List<FormPart> parts = request.getParts();
        for (FormPart part : parts) {
            if (part instanceof StringPart) {
                String value = ((StringPart) part).getValue();
                builder.addFormDataPart(part.getPartName(), value);
            } else if (part instanceof FilePart) {
                final FilePart filePart = (FilePart) part;
                final RequestBody requestBody = RequestBody.create(MediaType.parse(part.getContentType()),
                                                                   filePart.getFile());
                builder.addFormDataPart(filePart.getPartName(), filePart.getFileName(), requestBody);
            }
        }
        RequestBody multipartBody = builder.build();
        if (request.hasProgressListener()) {
            multipartBody = new ProgressRequestBody(multipartBody, request.getProgressListener());
        }
        return multipartBody;
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest request, TypeToken<ResourceType> resourceType)
            throws IOException, ApiRequestException, ApiMapperException {
        try {
            return mapResponse(fetchResponse(request), resourceType);

        } catch (ApiMapperException ex) {
            if (failFastOnMapper) {
                throw new RuntimeException(ex);
            } else {
                throw ex;
            }
        }
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest request, Class<ResourceType> resourceType)
            throws IOException, ApiRequestException, ApiMapperException {
        return fetchMappedResponse(request, TypeToken.of(resourceType));
    }

    @SuppressWarnings("unchecked")
    <T> T mapResponse(ApiResponse apiResponse, TypeToken<T> typeToken)
            throws IOException, ApiRequestException, ApiMapperException {
        if (apiResponse.isSuccess()) {
            if (apiResponse.hasResponseBody()) {
                return parseJsonResponse(apiResponse, typeToken);
            } else {
                throw new ApiMapperException("Empty response body");
            }
        } else {
            throw apiResponse.getFailure();
        }
    }

    private <T> T parseJsonResponse(ApiResponse apiResponse,
                                    TypeToken<T> typeToken) throws IOException, ApiMapperException {
        final T resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), typeToken);

        if (resource == null) {
            throw new ApiMapperException("Response could not be deserialized, or types do not match");
        }

        return resource;
    }

}
