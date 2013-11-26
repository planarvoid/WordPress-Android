package com.soundcloud.android.api;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(library = true, complete = false)
public class ApiModule {

    @Provides
    @Named("APIScheduler")
    public Scheduler provideApiScheduler(){
        return ScSchedulers.API_SCHEDULER;
    }

    @Provides
    @Singleton
    public JsonTransformer provideJsonTransformer() {
        return new JacksonJsonTransformer();
    }

    @Provides
    @Singleton
    public RxHttpClient provideRxHttpClient(@Named("APIScheduler") Scheduler scheduler, JsonTransformer jsonTransformer,
                                            SoundCloudApplication application, HttpProperties httpProperties) {
        return new SoundCloudRxHttpClient(scheduler, jsonTransformer, application, httpProperties);
    }

}
