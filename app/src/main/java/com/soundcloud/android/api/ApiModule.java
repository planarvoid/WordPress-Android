package com.soundcloud.android.api;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.rx.ScSchedulers;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(complete = false, library = true)
public class ApiModule {

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

    @Provides
    @Named("API")
    public Scheduler provideApiScheduler() {
        return ScSchedulers.API_SCHEDULER;
    }
}
