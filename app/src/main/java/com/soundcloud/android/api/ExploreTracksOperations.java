package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
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

import java.util.List;

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

    public Observable<Observable<ExploreTracksSuggestion>> getPopularMusic() {
        return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
    }

    public Observable<Observable<ExploreTracksSuggestion>> getPopularAudio() {
        return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
    }

    public Observable<Observable<ExploreTracksSuggestion>> getSuggestedTracksByCategory(ExploreTracksCategory category) {
        return getSuggestedTracks(category.getSuggestedTracksPath());
    }

    private Observable<Observable<ExploreTracksSuggestion>> getSuggestedTracks(String endpoint) {
        APIRequest<ModelCollection<ExploreTracksSuggestion>> request = SoundCloudAPIRequest.RequestBuilder.<ModelCollection<ExploreTracksSuggestion>>get(endpoint)
                .addQueryParameters("limit", "15")
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        return mRxHttpClient.fetchPagedModels(request);
    }

    public Observable<Track> getRelatedTracks(Track seedTrack) {
        APIRequest<List<Track>> request = SoundCloudAPIRequest.RequestBuilder.<List<Track>>get("/users/bad-panda-records/tracks.json")
                .addQueryParameters("limit", "100")
                .forPublicAPI()
                .forResource(new TypeToken<List<Track>>() {}).build();
        return mRxHttpClient.fetchModels(request);
    }

    private static class SuggestionsModelCollectionToken extends TypeToken<ModelCollection<ExploreTracksSuggestion>> {}
    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {}

}
