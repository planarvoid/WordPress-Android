package com.soundcloud.android.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksSuggestion;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.util.functions.Func1;

import java.util.List;

public class ExploreTracksOperations extends ScheduledOperations {


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

    public Observable<Observable<ExploreTracksSuggestion>> getSuggestedTracks(ExploreTracksCategory category) {
        if (category == ExploreTracksCategory.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreTracksCategory.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    private Observable<Observable<ExploreTracksSuggestion>> getSuggestedTracks(String endpoint) {
        APIRequest<ModelCollection<ExploreTracksSuggestion>> request = SoundCloudAPIRequest.RequestBuilder.<ModelCollection<ExploreTracksSuggestion>>get(endpoint)
                .addQueryParameters("limit", "15")
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        return mRxHttpClient.fetchPagedModels(request);
    }

    public Observable<Track> getRelatedTracks(final Track seedTrack) {
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), seedTrack.getUrn().toEncodedString());
        final APIRequest<ModelCollection<ExploreTracksSuggestion>> request = SoundCloudAPIRequest.RequestBuilder.<ModelCollection<ExploreTracksSuggestion>>get(endpoint)
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        return mRxHttpClient.<ModelCollection<ExploreTracksSuggestion>>fetchModels(request).mapMany(new Func1<ModelCollection<ExploreTracksSuggestion>, Observable<Track>>() {
            @Override
            public Observable<Track> call(ModelCollection<ExploreTracksSuggestion> exploreTracksSuggestionModelCollection) {
                List<Track> toEmit = Lists.newArrayListWithCapacity(exploreTracksSuggestionModelCollection.getCollection().size());
                if (!toEmit.isEmpty()) {
                    for (ExploreTracksSuggestion item : exploreTracksSuggestionModelCollection.getCollection()) {
                        toEmit.add(new Track(item));
                    }
                } else {
                    SoundCloudApplication.handleSilentException("Empty related tracks response from seed track " + seedTrack,
                            new IllegalStateException("No Related Tracks"));
                }
                return Observable.from(toEmit);
            }
        });
    }

    private static class SuggestionsModelCollectionToken extends TypeToken<ModelCollection<ExploreTracksSuggestion>> {
    }

    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {
    }

}
