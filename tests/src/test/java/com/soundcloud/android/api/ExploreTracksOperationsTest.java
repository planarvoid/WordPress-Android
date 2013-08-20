package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

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
}
