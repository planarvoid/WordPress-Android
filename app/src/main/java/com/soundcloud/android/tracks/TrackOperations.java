package com.soundcloud.android.tracks;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class TrackOperations {

    @SuppressWarnings("UnusedDeclaration")
    private static final String LOG_TAG = "TrackOperations";

    private final TrackStorage trackStorage;
    private final AccountOperations accountOperations;
    private final RxHttpClient rxHttpClient;

    @Inject
    public TrackOperations(TrackStorage trackStorage, AccountOperations accountOperations, RxHttpClient rxHttpClient) {
        this.trackStorage = trackStorage;
        this.accountOperations = accountOperations;
        this.rxHttpClient = rxHttpClient;
    }

    public Observable<PropertySet> track(final TrackUrn trackUrn) {
        return trackStorage.track(trackUrn, accountOperations.getLoggedInUserUrn());
    }

    public Observable<TrackDetails> trackDetailsWithUpdate(final TrackUrn trackUrn) {
        return Observable.concat(trackDetails(trackUrn), apiTrack(trackUrn).map(apiTrackToDetails));
    }

    private Observable<TrackDetails> trackDetails(final TrackUrn trackUrn) {
        return track(trackUrn).map(trackPropertyToDetails);
    }

    private Observable<PublicApiTrack> apiTrack(final TrackUrn trackUrn) {
        final String trackDetailsEndpoint = String.format(APIEndpoints.TRACK_DETAILS.path(), trackUrn.numericId);
        APIRequest<PublicApiTrack> request = SoundCloudAPIRequest.RequestBuilder.<PublicApiTrack>get(trackDetailsEndpoint)
                .forPublicAPI()
                .forResource(TypeToken.of(PublicApiTrack.class))
                .build();
        return rxHttpClient.fetchModels(request);
    }

    private Func1<PropertySet, TrackDetails> trackPropertyToDetails = new Func1<PropertySet, TrackDetails>() {
        @Override
        public TrackDetails call(PropertySet bindings) {
            return new TrackDetails(bindings);
        }
    };

    private Func1<PublicApiTrack, TrackDetails> apiTrackToDetails = new Func1<PublicApiTrack, TrackDetails>() {
        @Override
        public TrackDetails call(PublicApiTrack track) {
            return new TrackDetails(track);
        }
    };
}
