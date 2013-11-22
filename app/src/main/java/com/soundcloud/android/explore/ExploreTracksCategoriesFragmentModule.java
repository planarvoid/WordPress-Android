package com.soundcloud.android.explore;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(complete = false, injects = {ExploreTracksCategoriesFragment.class})
public class ExploreTracksCategoriesFragmentModule {

    @Provides
    AndroidObservableFactory provideFactory(ExploreTracksOperations exploreTracksOperations){
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }

    @Provides
    @Singleton
    public JsonTransformer provideJsonTransformer(){
        return new JacksonJsonTransformer();
    }

    @Provides
    @Singleton
    public RxHttpClient provideRxHttpClient(@Named("APIScheduler") Scheduler scheduler, JsonTransformer jsonTransformer,
                                            SoundCloudApplication application, HttpProperties httpProperties){
        return new SoundCloudRxHttpClient(scheduler, jsonTransformer, application, httpProperties);
    }
}
