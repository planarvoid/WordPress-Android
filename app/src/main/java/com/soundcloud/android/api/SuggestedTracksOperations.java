package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.api.Endpoints;
import rx.Observable;

public class SuggestedTracksOperations extends ScheduledOperations {


    private SoundCloudRxHttpClient rxHttpClient = new SoundCloudRxHttpClient();

    public Observable<Observable<Track>> getSuggestedTracks() {
        APIRequest<CollectionHolder<Track>> request = SoundCloudAPIRequest.RequestBuilder.<CollectionHolder<Track>>get(Endpoints.TRACKS + ".json")
                .addQueryParameters("linked_partitioning", "1")
                .forPublicAPI()
                .forResource(new TrackCollectionHolderToken()).build();
        return rxHttpClient.<Track>fetchPagedModels(request);
    }

    private static class TrackCollectionHolderToken extends TypeToken<CollectionHolder<Track>> {}
}
