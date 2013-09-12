package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksSuggestion;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    private ExploreTracksOperations exploreTracksOperations;
    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private Observer<ExploreTracksCategory> observer;

    @Before
    public void setUp() {
        exploreTracksOperations = new ExploreTracksOperations(soundCloudRxHttpClient);
    }

    @Test
    public void getCategoriesShouldMakeGetRequestToCategoriesEndpoint() {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getCategories().subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getUriPath()).toEqual(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path());
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToCategoriesEndpoint() {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        final Track seedTrack = new Track(123L);
        exploreTracksOperations.getRelatedTracks(seedTrack).subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getUriPath()).toEqual(String.format(APIEndpoints.RELATED_TRACKS.path(), seedTrack.getUrn().toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() {

        Observer<Track> relatedObserver = Mockito.mock(Observer.class);

        final ModelCollection<ExploreTracksSuggestion> collection = new ModelCollection<ExploreTracksSuggestion>();
        final ExploreTracksSuggestion suggestion1 = new ExploreTracksSuggestion("soundcloud:sounds:1");
        suggestion1.setUser(new UserSummary());
        final ExploreTracksSuggestion suggestion2 = new ExploreTracksSuggestion("soundcloud:sounds:2");
        suggestion2.setUser(new UserSummary());
        final ArrayList<ExploreTracksSuggestion> collection1 = Lists.newArrayList(suggestion1, suggestion2);
        collection.setCollection(collection1);

        when(soundCloudRxHttpClient.<ModelCollection<ExploreTracksSuggestion>>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(collection));
        final Track seedTrack = new Track(123L);
        exploreTracksOperations.getRelatedTracks(seedTrack).subscribe(relatedObserver);
        verify(relatedObserver).onNext(eq(new Track(suggestion1)));
        verify(relatedObserver).onNext(eq(new Track(suggestion2)));
        verify(relatedObserver).onCompleted();

    }
}
