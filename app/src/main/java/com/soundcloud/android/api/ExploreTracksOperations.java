package com.soundcloud.android.api;

import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.android.OperationPaged;
import rx.util.functions.Func1;

public class ExploreTracksOperations extends ScheduledOperations {

    private static final int PAGE_SIZE = 15;

    private SoundCloudRxHttpClient mRxHttpClient;

    public ExploreTracksOperations() {
        this(new SoundCloudRxHttpClient());
    }

    @VisibleForTesting
    protected ExploreTracksOperations(SoundCloudRxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<ExploreTracksCategories> getCategories() {
        APIRequest<ExploreTracksCategories> request = SoundCloudAPIRequest.RequestBuilder.<ExploreTracksCategories>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ExploreTracksCategories.class)).build();
        return mRxHttpClient.fetchModels(request);
    }

    public Observable<Page<SuggestedTracksCollection>> getSuggestedTracks(ExploreTracksCategory category) {
        if (category == ExploreTracksCategory.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreTracksCategory.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    private Observable<Page<SuggestedTracksCollection>> getSuggestedTracks(String endpoint) {
        APIRequest<SuggestedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<SuggestedTracksCollection>get(endpoint)
                .addQueryParameters("limit", String.valueOf(PAGE_SIZE))
                .forPrivateAPI(1)
                .forResource(TypeToken.of(SuggestedTracksCollection.class)).build();

        final Observable<SuggestedTracksCollection> source = mRxHttpClient.fetchModels(request);
        return Observable.create(paged(source, nextPageGenerator));
    }

    private final TrackSummariesNextPageFunc nextPageGenerator = new TrackSummariesNextPageFunc() {
        @Override
        public Observable<Page<SuggestedTracksCollection>> call(SuggestedTracksCollection trackSummaries) {
            final Optional<Link> nextLink = trackSummaries.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    public Observable<RelatedTracksCollection> getRelatedTracks(final Track seedTrack) {
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), seedTrack.getUrn().toEncodedString());
        final APIRequest<RelatedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<RelatedTracksCollection>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(RelatedTracksCollection.class)).build();

        return mRxHttpClient.fetchModels(request);
    }

    private interface TrackSummariesNextPageFunc extends Func1<SuggestedTracksCollection, Observable<Page<SuggestedTracksCollection>>> {}
}
