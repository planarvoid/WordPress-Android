package com.soundcloud.android.explore;

import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.android.OperationPaged;
import rx.util.functions.Func1;

import javax.inject.Inject;

public class ExploreTracksOperations extends ScheduledOperations {

    private static final int PAGE_SIZE = 15;

    public RxHttpClient mRxHttpClient;

    public ExploreTracksOperations(){
        this(new SoundCloudRxHttpClient());
    }
    @Inject
    public ExploreTracksOperations(RxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<ExploreGenresSections> getCategories() {
        APIRequest<ExploreGenresSections> request = SoundCloudAPIRequest.RequestBuilder.<ExploreGenresSections>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ExploreGenresSections.class)).build();
        return mRxHttpClient.fetchModels(request);
    }

    public Observable<Page<SuggestedTracksCollection>> getSuggestedTracks(ExploreGenre category) {
        if (category == ExploreGenre.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreGenre.POPULAR_AUDIO_CATEGORY) {
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

    private interface TrackSummariesNextPageFunc extends Func1<SuggestedTracksCollection, Observable<Page<SuggestedTracksCollection>>> {}
}
