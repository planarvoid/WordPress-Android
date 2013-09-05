package com.soundcloud.android.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksSuggestion;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScheduledOperations;
import com.sun.org.apache.regexp.internal.RE;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
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

    public Observable<Track> getRelatedTracks(Track seedTrack) {
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), seedTrack.getUrn().toEncodedString());
        final APIRequest<ModelCollection<ExploreTracksSuggestion>> request = SoundCloudAPIRequest.RequestBuilder.<ModelCollection<ExploreTracksSuggestion>>get(endpoint)
                .forPrivateAPI(1)
                .forResource(new SuggestionsModelCollectionToken()).build();

        return Observable.create(new Func1<Observer<Track>, Subscription>() {
            @Override
            public Subscription call(Observer<Track> trackObserver) {
                final Observable<ModelCollection<ExploreTracksSuggestion>> collectionObservable = mRxHttpClient.fetchModels(request);
                List<ExploreTracksSuggestion> suggestions = collectionObservable.toBlockingObservable().last().getCollection();
                for (ExploreTracksSuggestion item : suggestions) {
                    final Track track = new Track(item);
                    trackObserver.onNext(track);
                }
                return Subscriptions.empty();
            }
        });
    }

    private static class SuggestionsModelCollectionToken extends TypeToken<ModelCollection<ExploreTracksSuggestion>> {
    }

    private static class ExploreTracksCategoriesToken extends TypeToken<ExploreTracksCategories> {
    }

}
