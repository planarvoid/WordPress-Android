package com.soundcloud.android.api;

import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

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
    public RxHttpClient provideRxHttpClient(JsonTransformer jsonTransformer,
                                            Context context, HttpProperties httpProperties) {
        return new SoundCloudRxHttpClient(ScSchedulers.API_SCHEDULER, jsonTransformer, context, httpProperties);
    }

}
