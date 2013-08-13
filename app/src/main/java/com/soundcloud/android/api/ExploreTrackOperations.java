package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksCategorySection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class ExploreTrackOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient rxHttpClient = new SoundCloudRxHttpClient();

    public Observable<ExploreTracksCategory> getCategories() {

        return schedule(Observable.create(new Func1<Observer<ExploreTracksCategory>, Subscription>() {
            @Override
            public Subscription call(Observer<ExploreTracksCategory> categoryObserver) {
                try {
                    Thread.sleep(2000);
                    final String jsonString = IOUtils.readInputStream(SoundCloudApplication.instance.getAssets().open("suggested_tracks_categories.json"));
                    ExploreTracksCategories trackExploreCategories = new JacksonJsonTransformer().fromJson(
                            jsonString, new TypeToken<ExploreTracksCategories>() {
                    });

                    for (ExploreTracksCategory category : trackExploreCategories.getMusic()){
                        category.setSection(ExploreTracksCategorySection.MUSIC);
                        categoryObserver.onNext(category);
                    }

                    for (ExploreTracksCategory category : trackExploreCategories.getAudio()) {
                        category.setSection(ExploreTracksCategorySection.AUDIO);
                        categoryObserver.onNext(category);
                    }
                    categoryObserver.onCompleted();

                } catch (Exception e) {
                    e.printStackTrace();
                    categoryObserver.onError(e);
                }
                return Subscriptions.empty();
            }
        }).subscribeOn(ScSchedulers.API_SCHEDULER));
    }

    public Observable<Observable<Track>> getSuggestedTracks() {
        APIRequest<CollectionHolder<Track>> request = SoundCloudAPIRequest.RequestBuilder.<CollectionHolder<Track>>get("/users/skrillex/tracks.json")
                .addQueryParameters("linked_partitioning", "1")
                .addQueryParameters("limit", "10")
                .forPublicAPI()
                .forResource(new TrackCollectionHolderToken()).build();
        return rxHttpClient.<Track>fetchPagedModels(request);
    }

    private static class TrackCollectionHolderToken extends TypeToken<CollectionHolder<Track>> {}
}
