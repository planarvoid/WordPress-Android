package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersOperationsTest {

    private SuggestedUsersOperations suggestedUsersOperations;

    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private UserAssociationStorage userAssociationStorage;
    @Mock
    private CategoryGroup categoryGroupOne;
    @Mock
    private CategoryGroup categoryGroupTwo;
    @Mock
    private Observer<CategoryGroup> observer;
    @Mock
    private Observable<Object> observable;

    @Before
    public void setUp(){
        suggestedUsersOperations = new SuggestedUsersOperations(soundCloudRxHttpClient);
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getUriPath()).toEqual("/app/mobileapps/suggestions/users/social/facebook");
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getUriPath()).toEqual("/app/mobileapps/suggestions/users/categories");
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldReturnEmptyCategoryWhenFacebookFails() {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(TestObservables.errorThrowingObservable(new RuntimeException()));
        suggestedUsersOperations.getFacebookSuggestions().subscribe(observer);
        verify(observer, never()).onError(any(Exception.class));
    }

}
