package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.TrackSummary;
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
import java.util.Iterator;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    private ExploreTracksOperations exploreTracksOperations;
    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private Observer observer;

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
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        exploreTracksOperations.getRelatedTracks(123L).subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getUriPath()).toEqual(String.format(APIEndpoints.RELATED_TRACKS.path(),
                ClientUri.fromTrack(123L).toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() {

        Observer<ModelCollection<TrackSummary>> relatedObserver = Mockito.mock(Observer.class);

        final ModelCollection<TrackSummary> collection = new ModelCollection<TrackSummary>();
        final TrackSummary suggestion1 = new TrackSummary("soundcloud:sounds:1");
        suggestion1.setUser(new UserSummary());
        final TrackSummary suggestion2 = new TrackSummary("soundcloud:sounds:2");
        suggestion2.setUser(new UserSummary());
        final ArrayList<TrackSummary> collection1 = Lists.newArrayList(suggestion1, suggestion2);
        collection.setCollection(collection1);

        when(soundCloudRxHttpClient.<ModelCollection<TrackSummary>>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(collection));
        exploreTracksOperations.getRelatedTracks(123L).subscribe(relatedObserver);

        ArgumentCaptor<ModelCollection> argumentCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(relatedObserver).onNext(argumentCaptor.capture());
        Iterator iterator = argumentCaptor.getValue().iterator();
        expect(iterator.next()).toEqual(suggestion1);
        expect(iterator.next()).toEqual(suggestion2);
        verify(relatedObserver).onCompleted();
        verify(relatedObserver, never()).onError(any(Throwable.class));

    }
}
