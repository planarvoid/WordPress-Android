package com.soundcloud.android.api;

import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.android.OperationPaged;
import rx.util.functions.Func1;

import java.util.List;

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
                .forResource(new ExploreTracksCategoriesToken()).build();
        return mRxHttpClient.fetchModels(request);
    }

    public Observable<Page<TrackSummary>> getSuggestedTracks(ExploreTracksCategory category) {
        if (category == ExploreTracksCategory.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreTracksCategory.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    private TrackSummariesNextPageFunc nextPageGenerator = new TrackSummariesNextPageFunc() {
        @Override
        public Observable<Page<TrackSummary>> call(TrackSummaries trackSummaries) {
            final Optional<Link> nextLink = trackSummaries.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    private Observable<Page<TrackSummary>> getSuggestedTracks(String endpoint) {
        APIRequest<TrackSummaries> request = SoundCloudAPIRequest.RequestBuilder.<TrackSummaries>get(endpoint)
                .addQueryParameters("limit", String.valueOf(PAGE_SIZE))
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        final Observable<TrackSummaries> source = mRxHttpClient.fetchModels(request);
        return Observable.create(paged(source, nextPageGenerator));
    }

    public Observable<Track> getRelatedTracks(final Track seedTrack) {
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), seedTrack.getUrn().toEncodedString());
        final APIRequest<TrackSummaries> request = SoundCloudAPIRequest.RequestBuilder.<TrackSummaries>get(endpoint)
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        return mRxHttpClient.<ModelCollection<TrackSummary>>fetchModels(request).mapMany(new Func1<ModelCollection<TrackSummary>, Observable<Track>>() {
            @Override
            public Observable<Track> call(ModelCollection<TrackSummary> exploreTracksSuggestionModelCollection) {
                List<Track> toEmit = Lists.newArrayListWithCapacity(PAGE_SIZE);
                for (TrackSummary item : exploreTracksSuggestionModelCollection) {
                    toEmit.add(new Track(item));
                }
                return Observable.from(toEmit);
            }
        });
    }

    private static class SuggestionsModelCollectionToken extends TypeToken<TrackSummaries> {
    }

    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {
    }

    private static class TrackSummaries extends ModelCollection<TrackSummary> {}

    private interface TrackSummariesNextPageFunc extends Func1<TrackSummaries, Observable<Page<TrackSummary>>> {}
}
