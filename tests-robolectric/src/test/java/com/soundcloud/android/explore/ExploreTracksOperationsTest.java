package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    private ExploreTracksOperations exploreTracksOperations;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private Observer observer;
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() {
        exploreTracksOperations = new ExploreTracksOperations(storeTracksCommand, apiClientRx, Schedulers.immediate());
    }

    @Test
    public void getCategoriesShouldMakeGetRequestToCategoriesEndpoint() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getCategories().subscribe(observer);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.EXPLORE_TRACKS_CATEGORIES.path())));
    }

    @Test
    public void getPopularMusicShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_MUSIC_CATEGORY).subscribe(observer);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path())));
    }

    @Test
    public void getPopularAudioShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY).subscribe(observer);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path())));
    }

    @Test
    public void getTracksByCategoryShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());
        String tracksUrl = "/suggestions/tracks/electronic";
        ExploreGenre genre = new ExploreGenre("title", tracksUrl);

        exploreTracksOperations.getSuggestedTracks(genre).subscribe(observer);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", tracksUrl)));
    }

    @Test
    public void shouldWriteSuggestedTracksInLocalStorage() throws Exception {
        SuggestedTracksCollection collection = buildSuggestedTracksResponse();

        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_MUSIC_CATEGORY).subscribe(observer);

        expect(storeTracksCommand.getInput()).toBe(collection);
        verify(storeTracksCommand).call();
    }

    private SuggestedTracksCollection buildSuggestedTracksResponse() throws CreateModelException {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        SuggestedTracksCollection collection = new SuggestedTracksCollection();
        collection.setCollection(Arrays.asList(track));
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(
                Observable.just(collection));
        return collection;
    }
}
