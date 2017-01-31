package com.soundcloud.android.api;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LocaleFormatter;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Module
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
                                      LocaleFormatter localeFormatter) {
        ApiClient apiClient = new ApiClient(httpClient, urlBuilder, jsonTransformer, deviceHelper, adIdHelper,
                                            oAuth, unauthorisedRequestRegistry, accountOperations, localeFormatter);
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
    @Singleton
    public OkHttpClient.Builder provideOkHttpClientBuilder(ApiUserPlanInterceptor userPlanInterceptor,
                                                           ApplicationProperties applicationProperties) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(userPlanInterceptor);

        if (applicationProperties.isDevelopmentMode()) {
            clientBuilder.addNetworkInterceptor(new StethoInterceptor());
        }

        return clientBuilder;
    }

    @Provides
    @Singleton
    @Named(API_HTTP_CLIENT)
    public OkHttpClient provideOkHttpClient(OkHttpClient.Builder clientBuilder) {
        return clientBuilder.build();
    }
}
