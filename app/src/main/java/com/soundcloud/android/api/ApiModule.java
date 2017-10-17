package com.soundcloud.android.api;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LocaleFormatter;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import android.content.Context;
import android.content.res.Resources;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Module
public class ApiModule {

    public static final String EVENTGATEWAY_BASE_URL = "EventgatewayBaseUrl";
    protected static final String PUBLIC_API_BASE_URL = "PublicApiBaseUrl";
    protected static final String MOBILE_API_BASE_URL = "MobileApiBaseUrl";
    private static final int READ_WRITE_TIMEOUT_SECONDS = 20;
    private static final int CONNECT_TIMEOUT_SECONDS = 20;

    @Provides
    @Singleton
    @Named(PUBLIC_API_BASE_URL)
    protected String providePublicApiBaseUrl(Resources resources) {
        return resources.getString(R.string.public_api_base_url);
    }

    @Provides
    @Singleton
    @Named(MOBILE_API_BASE_URL)
    protected String provideMobileApiBaseUrl(Resources resources) {
        return resources.getString(R.string.mobile_api_base_url);
    }

    @Provides
    @Singleton
    @Named(EVENTGATEWAY_BASE_URL)
    protected String provideEventgatewayBaseUrl(Resources resources) {
        return resources.getString(R.string.eventgateway_url);
    }

    @Provides
    static ApiClient provideApiClient(OkHttpClient httpClient,
                                      ApiUrlBuilder urlBuilder,
                                      JsonTransformer jsonTransformer,
                                      DeviceHelper deviceHelper,
                                      AdIdHelper adIdHelper,
                                      OAuth oAuth,
                                      UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                                      AccountOperations accountOperations,
                                      LocaleFormatter localeFormatter,
                                      ApplicationProperties applicationProperties,
                                      ExperimentOperations experimentOperations) {
        ApiClient apiClient = new ApiClient(httpClient, urlBuilder, jsonTransformer, deviceHelper, adIdHelper,
                                            oAuth, unauthorisedRequestRegistry, accountOperations, localeFormatter,
                                            applicationProperties.shouldFailFastOnMappingExceptions(), experimentOperations);
        apiClient.setAssertBackgroundThread(true);
        return apiClient;
    }

    @Provides
    static Locale provideDefaultLocale() {
        return Locale.getDefault();
    }

    @Provides
    @Singleton
    static JsonTransformer provideJsonTransformer() {
        return new JacksonJsonTransformer();
    }

    @Provides
    @Singleton
    static UnauthorisedRequestRegistry provideUnauthorizedRequestRegistry(Context context) {
        return UnauthorisedRequestRegistry.getInstance(context);
    }

    /**
     * Returns an OkHttpClient with default settings appropriate for most uses in the app. Customized clients can be
     * created by injecting this client and using {@link OkHttpClient#newBuilder()}. This will ensure all clients in
     * the app share a single connection pool.
     *
     * <p>
     *   <strong>Note that returning a {@link OkHttpClient.Builder} here would not be safe!</strong> If we were to
     *   return a singleton builder, any customizations to that builder would be global. Alternatively, if we
     *   were to create a new builder instance each time, they would not share a connection pool.
     * </p>
     */
    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(ApiUserPlanInterceptor userPlanInterceptor,
                                                           ApplicationProperties applicationProperties) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(userPlanInterceptor);

        if (applicationProperties.isDevelopmentMode()) {
            clientBuilder.addNetworkInterceptor(new StethoInterceptor());
        }

        return clientBuilder.build();
    }
}
