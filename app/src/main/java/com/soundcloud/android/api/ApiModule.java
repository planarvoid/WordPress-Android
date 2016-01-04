package com.soundcloud.android.api;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LocaleHeaderFormatter;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Module(complete = false, library = true)
public class ApiModule {

    public static final String API_HTTP_CLIENT = "ApiHttpClient";

    private static final int READ_WRITE_TIMEOUT_SECONDS = 20;
    private static final int CONNECT_TIMEOUT_SECONDS = 20;

    @Provides
    public ApiClient provideApiClient(@Named(API_HTTP_CLIENT) OkHttpClient httpClient,
                                      ApiUrlBuilder urlBuilder,
                                      JsonTransformer jsonTransformer,
                                      DeviceHelper deviceHelper,
                                      AdIdHelper adIdHelper,
                                      OAuth oAuth,
                                      UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                                      AccountOperations accountOperations,
                                      LocaleHeaderFormatter localeHeaderFormatter) {
        ApiClient apiClient = new ApiClient(httpClient, urlBuilder, jsonTransformer, deviceHelper, adIdHelper,
                oAuth, unauthorisedRequestRegistry, accountOperations, localeHeaderFormatter);
        apiClient.setAssertBackgroundThread(true);
        return apiClient;
    }

    @Provides
    public Locale provideDefaultLocale() {
        return Locale.getDefault();
    }

    @Provides
    @Singleton
    public JsonTransformer provideJsonTransformer() {
        return new JacksonJsonTransformer();
    }

    @Provides
    @Singleton
    public UnauthorisedRequestRegistry provideUnauthorizedRequestRegistry(Context context) {
        return UnauthorisedRequestRegistry.getInstance(context);
    }

    @Provides
    public PublicApi providePublicCloudApi(Context context) {
        return PublicApi.getInstance(context);
    }

    @Provides
    @Singleton
    @Named(API_HTTP_CLIENT)
    public OkHttpClient provideOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return okHttpClient;
    }
}
