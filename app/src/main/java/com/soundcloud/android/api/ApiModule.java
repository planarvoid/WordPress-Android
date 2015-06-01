package com.soundcloud.android.api;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Singleton;

@Module(complete = false, library = true)
public class ApiModule {

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
    public PublicCloudAPI providePublicCloudApi(Context context) {
        return new PublicApi(context);
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }
}
