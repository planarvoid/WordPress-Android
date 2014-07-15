package com.soundcloud.android.explore;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.SoundCloudRxHttpClient;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    private ExploreTracksOperations exploreTracksOperations;

    @Mock
    private SoundCloudRxHttpClient rxHttpClient;
    @Mock
    private TrackWriteStorage trackWriteStorage;
    @Mock
    private Observer observer;

    @Before
    public void setUp() {
        exploreTracksOperations = new ExploreTracksOperations(rxHttpClient, trackWriteStorage);
    }

    @Test
    public void getCategoriesShouldMakeGetRequestToCategoriesEndpoint() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getCategories().subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())));
    }

    @Test
    public void getPopularMusicShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_MUSIC_CATEGORY).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path())));
    }

    @Test
    public void getPopularAudioShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_AUDIO_CATEGORY).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path())));
    }

    @Test
    public void getTracksByCategoryShouldMakeGetRequestToMobileApiEndpoint() throws Exception {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        String tracksUrl = "/suggestions/tracks/electronic";
        ExploreGenre genre = new ExploreGenre("title", tracksUrl);

        exploreTracksOperations.getSuggestedTracks(genre).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", tracksUrl)));
    }

    @Test
    public void shouldWriteSuggestedTracksInLocalStorage() throws Exception {
        SuggestedTracksCollection collection = buildSuggestedTracksResponse();

        exploreTracksOperations.getSuggestedTracks(ExploreGenre.POPULAR_MUSIC_CATEGORY).subscribe(observer);

        verify(trackWriteStorage).storeTracksAsync(collection.getCollection());
    }

    private SuggestedTracksCollection buildSuggestedTracksResponse() throws CreateModelException {
        ApiTrack track = TestHelper.getModelFactory().createModel(ApiTrack.class);
        SuggestedTracksCollection collection = new SuggestedTracksCollection();
        collection.setCollection(Arrays.asList(track));
        when(rxHttpClient.<SuggestedTracksCollection>fetchModels(any(APIRequest.class))).thenReturn(
                Observable.<SuggestedTracksCollection>from(collection));
        return collection;
    }
}
