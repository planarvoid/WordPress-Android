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
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class ExploreTrackOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient rxHttpClient = new SoundCloudRxHttpClient();

    public Observable<ExploreTracksCategories> getCategories() {
        return schedule(Observable.create(new Func1<Observer<ExploreTracksCategories>, Subscription>() {
            @Override
            public Subscription call(Observer<ExploreTracksCategories> suggestedTrackObserver) {
                try {
                    final String jsonString = IOUtils.readInputStream(SoundCloudApplication.instance.getAssets().open("suggested_tracks_categories.json"));
                    ExploreTracksCategories trackExploreCategories = new JacksonJsonTransformer().fromJson(
                            jsonString, new TypeToken<ExploreTracksCategories>() {
                    });
                    suggestedTrackObserver.onNext(trackExploreCategories);
                    suggestedTrackObserver.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    suggestedTrackObserver.onError(e);
                }
                return Subscriptions.empty();
            }
        }));
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
