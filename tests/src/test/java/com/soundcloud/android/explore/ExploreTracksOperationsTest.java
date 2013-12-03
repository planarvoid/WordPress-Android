package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import dagger.Module;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import javax.inject.Inject;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    @Inject
    ExploreTracksOperations exploreTracksOperations;
    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private Observer observer;

    @Before
    public void setUp() {
        MockInjector.create(new TestModule()).inject(this);
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

    @Module(injects = ExploreTracksOperationsTest.class)
    public class TestModule {

        @Provides
        RxHttpClient provideHttpClient() {
            return soundCloudRxHttpClient;
        }
    }
}
