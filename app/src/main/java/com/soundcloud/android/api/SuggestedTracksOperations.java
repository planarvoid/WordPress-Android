package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;

public class SuggestedTracksOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient rxHttpClient = new SoundCloudRxHttpClient();

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
