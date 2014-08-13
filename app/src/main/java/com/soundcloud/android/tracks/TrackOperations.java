package com.soundcloud.android.tracks;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;

public class TrackOperations {

    @SuppressWarnings("UnusedDeclaration")
    private static final String LOG_TAG = "TrackOperations";

    private final TrackStorage trackStorage;
    private final AccountOperations accountOperations;
    private final RxHttpClient rxHttpClient;
    private final BulkStorage bulkStorage;
    private final EventBus eventBus;

    private final Func1<PublicApiTrack, PropertySet> apiTrackToPropertySet = new Func1<PublicApiTrack, PropertySet>() {
        @Override
        public PropertySet call(PublicApiTrack track) {
            return track.toPropertySet();
        }
    };

    private final Action1<PublicApiTrack> storeApiTrack = new Action1<PublicApiTrack>() {
        @Override
        public void call(PublicApiTrack publicApiTrack) {
            bulkStorage.bulkInsert(Lists.newArrayList(publicApiTrack));
        }
    };
    private Action1<PropertySet> publishPlayableChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet propertySet) {
            final PlayableUpdatedEvent event = PlayableUpdatedEvent.forUpdate(propertySet.get(TrackProperty.URN), propertySet);
            eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);
        }
    };

    @Inject
    public TrackOperations(TrackStorage trackStorage, AccountOperations accountOperations, RxHttpClient rxHttpClient, BulkStorage bulkStorage, EventBus eventBus) {
        this.trackStorage = trackStorage;
        this.accountOperations = accountOperations;
        this.rxHttpClient = rxHttpClient;
        this.bulkStorage = bulkStorage;
        this.eventBus = eventBus;
    }

    public Observable<PropertySet> track(final TrackUrn trackUrn) {
        return trackStorage.track(trackUrn, accountOperations.getLoggedInUserUrn());
    }

    public Observable<PropertySet> trackDetailsWithUpdate(final TrackUrn trackUrn) {
        return Observable.concat(track(trackUrn), apiTrack(trackUrn)
                .doOnNext(storeApiTrack)
                .map(apiTrackToPropertySet)
                .doOnNext(publishPlayableChanged));
    }

    private Observable<PublicApiTrack> apiTrack(final TrackUrn trackUrn) {
        final String trackDetailsEndpoint = String.format(APIEndpoints.TRACK_DETAILS.path(), trackUrn.numericId);
        APIRequest<PublicApiTrack> request = SoundCloudAPIRequest.RequestBuilder.<PublicApiTrack>get(trackDetailsEndpoint)
                .forPublicAPI()
                .forResource(TypeToken.of(PublicApiTrack.class))
                .build();
        return rxHttpClient.fetchModels(request);
    }
}
