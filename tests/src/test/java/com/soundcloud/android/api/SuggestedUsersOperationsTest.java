package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.observables.BlockingObservable;

import java.util.Collection;
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
    private UserAssociation userAssociationsOne;
    @Mock
    private UserAssociation userAssociationsTwo;
    @Mock
    private Observable<Object> observable;
    private Collection<UserAssociation> userAssociations;

    @Before
    public void setUp(){
        initMocks(this);
        userAssociations = Lists.newArrayList(userAssociationsOne, userAssociationsTwo);
        suggestedUsersOperations = new SuggestedUsersOperations(soundCloudRxHttpClient, userAssociationStorage);
        when(soundCloudRxHttpClient.executeAPIRequest(any(APIRequest.class))).thenReturn(Observable.empty());
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getUriPath()).toEqual("/app/mobileapps/suggestions/users/social/facebook");
    }

    @Test
    public void shouldMakeRequestToFacebookSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromFacebookSuggestionsEndpoint(){
        suggestedUsersOperations.getFacebookSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getUriPath()).toEqual("/app/mobileapps/suggestions/users/categories");
    }

    @Test
    public void shouldMakeRequestToSoundSuggestionsEndpointVersion1(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getVersion()).toEqual(1);
    }

    @Test
    public void shouldMakeGetRequestToSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
    }

    @Test
    public void shouldRequestCollectionOfCategoriesFromSoundSuggestionsEndpoint(){
        suggestedUsersOperations.getMusicAndSoundsSuggestions();
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getResourceType()).toEqual(new TypeToken<List<CategoryGroup>>(){});
    }

    @Test
    public void shouldReturnEmptyCategoryWhenFacebookFails() {
        when(soundCloudRxHttpClient.executeAPIRequest(any(APIRequest.class))).thenReturn(TestObservables.errorThrowingObservable(new RuntimeException()));
        suggestedUsersOperations.getFacebookSuggestions().subscribe(observer);
        verify(observer, never()).onError(any(Exception.class));
    }

    @Test
    public void shouldReturnTrueIfNoAssociationHasToken(){
        expect(suggestedUsersOperations.bulkFollowAssociations(userAssociations)).toBeTrue();
        verifyZeroInteractions(soundCloudRxHttpClient);
        verifyZeroInteractions(userAssociationStorage);

    }

    @Test
    public void shouldMakeAPostRequestWhenBulkFollowing() {
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        suggestedUsersOperations.bulkFollowAssociations(userAssociations);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("POST");
    }

    @Test
    public void shouldMakeRequestToPublicAPIWhenBulkFollowing() {
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        suggestedUsersOperations.bulkFollowAssociations(userAssociations);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        expect(argumentCaptor.getValue().isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddTokensAsQueryParametersWhenBulkFollowing() {
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsTwo.getToken()).thenReturn("token2");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        when(userAssociationsTwo.hasToken()).thenReturn(true);
        suggestedUsersOperations.bulkFollowAssociations(userAssociations);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).executeAPIRequest(argumentCaptor.capture());
        Multimap<String, String> queryParameters = argumentCaptor.getValue().getQueryParameters();
        expect(queryParameters.get("tokens")).toContainExactly("token1", "token2");
    }

    @Test
    public void shouldMakeObservableBlockingWhenBulkFollowing(){
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        when(soundCloudRxHttpClient.executeAPIRequest(any(APIRequest.class))).thenReturn(observable);
        when(observable.toBlockingObservable()).thenReturn(mock(BlockingObservable.class));
        suggestedUsersOperations.bulkFollowAssociations(userAssociations);
        verify(observable).toBlockingObservable();
    }

    @Test
    public void shouldSetAssociationsAsSyncedIfBulkFollowRequestIsSuccessful(){
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        suggestedUsersOperations.bulkFollowAssociations(userAssociations);
        verify(userAssociationStorage).setFollowingAsSynced(userAssociationsOne);
    }

    @Test
    public void shouldReturnFalseIfErrorRaisedDuringBulkFollowingRequest(){
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        when(userAssociationStorage.setFollowingAsSynced(userAssociationsOne)).thenThrow(RuntimeException.class);
        expect(suggestedUsersOperations.bulkFollowAssociations(userAssociations)).toBeFalse();
    }

    @Test
    public void shouldReturnTrueIfBulkFollowingRequestSucceeds(){
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        expect(suggestedUsersOperations.bulkFollowAssociations(userAssociations)).toBeTrue();
    }
}
