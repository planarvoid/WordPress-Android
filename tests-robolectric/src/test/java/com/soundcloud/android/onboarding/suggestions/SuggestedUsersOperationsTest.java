package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.UserAssociationStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersOperationsTest {

    private SuggestedUsersOperations suggestedUsersOperations;

    @Mock private ApiClientRx apiClientRx;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private CategoryGroup categoryGroupOne;
    @Mock private CategoryGroup categoryGroupTwo;
    @Mock private Observer<CategoryGroup> observer;
    @Mock private Observable<Object> observable;

    @Before
    public void setUp(){
        suggestedUsersOperations = new SuggestedUsersOperations(apiClientRx, Schedulers.immediate());
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getEncodedPath()).toEqual("/suggestions/users/social/facebook");
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getEncodedPath()).toEqual("/suggestions/users/categories");
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldReturnEmptyCategoryWhenFacebookFails() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.error(new Exception()));
        suggestedUsersOperations.getFacebookSuggestions().subscribe(observer);
        verify(observer, never()).onError(any(Exception.class));
    }

}
