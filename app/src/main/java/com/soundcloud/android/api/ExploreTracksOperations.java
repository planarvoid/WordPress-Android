package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;

public class ExploreTracksOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient mRxHttpClient;

    public ExploreTracksOperations(){
        this(new SoundCloudRxHttpClient());
    }

    public ExploreTracksOperations(SoundCloudRxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<ExploreTracksCategories> getCategories() {
        APIRequest<ExploreTracksCategories> request = SoundCloudAPIRequest.RequestBuilder.<ExploreTracksCategories>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new ExploreTracksCategoriesToken()).build();
        return mRxHttpClient.fetchModels(request);
    }

    public Observable<Observable<Track>> getSuggestedTracks() {
        APIRequest<CollectionHolder<Track>> request = SoundCloudAPIRequest.RequestBuilder.<CollectionHolder<Track>>get("/users/skrillex/tracks.json")
                .addQueryParameters("linked_partitioning", "1")
                .addQueryParameters("limit", "10")
                .forPublicAPI()
                .forResource(new TrackCollectionHolderToken()).build();
        return mRxHttpClient.<Track>fetchPagedModels(request);
    }

    private static class TrackCollectionHolderToken extends TypeToken<CollectionHolder<Track>> {}
    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {}

}
