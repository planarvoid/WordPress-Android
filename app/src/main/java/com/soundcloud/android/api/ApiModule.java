package com.soundcloud.android.api;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Module(complete = false, library = true)
public class ApiModule {

    private static final int READ_WRITE_TIMEOUT_SECONDS = 20;
    private static final int CONNECT_TIMEOUT_SECONDS = 20;

    @Provides
    public ApiClient provideApiClient(OkHttpClient httpClient,
                                      ApiUrlBuilder urlBuilder,
                                      JsonTransformer jsonTransformer,
                                      DeviceHelper deviceHelper,
                                      AdIdHelper adIdHelper,
                                      OAuth oAuth,
                                      UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                                      AccountOperations accountOperations) {
        ApiClient apiClient = new ApiClient(httpClient, urlBuilder, jsonTransformer, deviceHelper, adIdHelper,
                oAuth, unauthorisedRequestRegistry, accountOperations);
        apiClient.setAssertBackgroundThread(true);
        return apiClient;
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
    public OkHttpClient provideOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return okHttpClient;
    }
}
