package com.soundcloud.android.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksCategorySection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.List;

public class ExploreTracksOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient rxHttpClient = new SoundCloudRxHttpClient();

    public Observable<ExploreTracksCategory> getCategories() {
        APIRequest<ExploreTracksCategories> request = SoundCloudAPIRequest.RequestBuilder.<ExploreTracksCategories>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new ExploreTracksCategoriesToken()).build();
        Observable<ExploreTracksCategories> categoriesObservable = rxHttpClient.fetchModels(request);

        return categoriesObservable.mapMany(new Func1<ExploreTracksCategories, Observable<ExploreTracksCategory>>() {
            @Override
            public Observable<ExploreTracksCategory> call(final ExploreTracksCategories exploreTracksCategories) {
                return Observable.create(new Func1<Observer<ExploreTracksCategory>, Subscription>() {
                    @Override
                    public Subscription call(Observer<ExploreTracksCategory> exploreTracksCategoryObserver) {
                        for (ExploreTracksCategory category : exploreTracksCategories.mMusic) {
                            category.setSection(ExploreTracksCategorySection.MUSIC);
                            exploreTracksCategoryObserver.onNext(category);
                        }

                        for (ExploreTracksCategory category : exploreTracksCategories.mAudio) {
                            category.setSection(ExploreTracksCategorySection.AUDIO);
                            exploreTracksCategoryObserver.onNext(category);
                        }
                        exploreTracksCategoryObserver.onCompleted();
                        return Subscriptions.empty();
                    }
                });
            }
        });
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
    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {}

    private static class ExploreTracksCategories {

        private List<ExploreTracksCategory> mMusic;
        private List<ExploreTracksCategory> mAudio;

        @JsonProperty("audio")
        public void setAudio(List<ExploreTracksCategory> audio) {
            this.mAudio = audio;
        }

        @JsonProperty("music")
        public void setMusic(List<ExploreTracksCategory> music) {
            this.mMusic = music;
        }
    }
}
