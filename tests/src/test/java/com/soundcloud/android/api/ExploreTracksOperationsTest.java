package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksCategorySection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.List;

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
    public void getCategoriesShouldEmitNoCategories() {
        ExploreTracksOperations.ExploreTracksCategories exploreTracksCategories = new ExploreTracksOperations.ExploreTracksCategories();

        when(soundCloudRxHttpClient.<ExploreTracksOperations.ExploreTracksCategories>fetchModels(any(APIRequest.class)))
                .thenReturn(Observable.just(exploreTracksCategories));

        List emittedCategories = Lists.newArrayList(exploreTracksOperations.getCategories().toBlockingObservable().toIterable());
        expect(emittedCategories.size()).toBe(0);

    }

    @Test
    public void getCategoriesShouldEmitCategoriesWithMappedSections() {

        ExploreTracksOperations.ExploreTracksCategories exploreTracksCategories = new ExploreTracksOperations.ExploreTracksCategories();
        exploreTracksCategories.setMusic(Lists.newArrayList(new ExploreTracksCategory("music1")));
        exploreTracksCategories.setAudio(Lists.newArrayList(new ExploreTracksCategory("audio1")));

        when(soundCloudRxHttpClient.<ExploreTracksOperations.ExploreTracksCategories>fetchModels(any(APIRequest.class)))
                .thenReturn(Observable.just(exploreTracksCategories));

        List<ExploreTracksCategory> emittedCategories = Lists.newArrayList(exploreTracksOperations.getCategories().toBlockingObservable().toIterable());
        expect(emittedCategories.size()).toBe(2);
        expect(emittedCategories.get(0).getTitle()).toBe("music1");
        expect(emittedCategories.get(0).getSection()).toBe(ExploreTracksCategorySection.MUSIC);
        expect(emittedCategories.get(1).getTitle()).toBe("audio1");
        expect(emittedCategories.get(1).getSection()).toBe(ExploreTracksCategorySection.AUDIO);

    }
}
