package com.soundcloud.android.explore;

import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.dagger.AndroidObservableFactory;
import com.soundcloud.android.rx.ScSchedulers;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(complete = false, injects = {ExploreTracksCategoriesFragment.class})
public class ExploreTracksCategoriesModule {

    @Provides
    AndroidObservableFactory provideFactory(ExploreTracksOperations exploreTracksOperations){
        return new AndroidObservableFactory(exploreTracksOperations.getCategories());
    }

    @Provides
    @Named("APIScheduler")
    public Scheduler provideApiScheduler(){
        return ScSchedulers.API_SCHEDULER;
    }

    @Provides
    @Singleton
    public JsonTransformer provideJsonTransformer(){
        return new JacksonJsonTransformer();
    }

}
