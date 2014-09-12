package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.SoundCloudRxHttpClient;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(SoundCloudTestRunner.class)
public class FollowingOperationsTest {

    private FollowingOperations ops;

    @Mock
    private UserAssociationStorage userAssociationStorage;
    @Mock
    private FollowStatus followStatus;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private ScModelManager scModelManager;
    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private SyncInitiator syncInitiator;
    @Mock
    private UserAssociation userAssociationOne;
    @Mock
    private UserAssociation userAssociationTwo;
    @Mock
    private Observer observer;

    private PublicApiUser user;
    private SuggestedUser suggestedUser;
    private List<SuggestedUser> suggestedUsers;
    private Collection<UserAssociation> userAssociations;

    @Before
    public void before() throws CreateModelException {
        when(scModelManager.cache(any(PublicApiUser.class), any(PublicApiResource.CacheUpdateMode.class))).thenReturn(mock(PublicApiUser.class));

        Observable<UserAssociation> observable = Observable.just(userAssociationOne);
        when(userAssociationStorage.follow(any(PublicApiUser.class))).thenReturn(observable);
        when(userAssociationStorage.unfollow(any(PublicApiUser.class))).thenReturn(observable);

        ops = new FollowingOperations(soundCloudRxHttpClient, userAssociationStorage, syncStateManager, followStatus, scModelManager, syncInitiator);

        user = ModelFixtures.create(PublicApiUser.class);

        suggestedUser = ModelFixtures.create(SuggestedUser.class);
        suggestedUsers = ModelFixtures.create(SuggestedUser.class, 3);

        userAssociations = Lists.newArrayList(userAssociationOne, userAssociationTwo);
    }

    @Test
    public void shouldToggleFollowingOnAddition() throws CreateModelException {
        ops.addFollowing(user).subscribe(observer);
        verify(followStatus).toggleFollowing(user.getId());
        verify(observer).onNext(true);
    }

    @Test
    public void shouldSyncFollowingsOnAddition() {
        ops.addFollowing(user).subscribe(observer);
        verify(syncInitiator).pushFollowingsToApi();
    }

    @Test
    public void shouldToggleFollowingOnSuggestedUserAddition() throws CreateModelException {
        ops.addFollowingBySuggestedUser(suggestedUser);
        verify(followStatus).toggleFollowing(suggestedUser.getId());
    }

    @Test
    public void shouldToggleFollowingsOnSuggestedUserAdditions() throws CreateModelException {
        ops.addFollowingsBySuggestedUsers(suggestedUsers);
        verify(followStatus).toggleFollowing(ScModel.getIdList(suggestedUsers));
    }

    @Test
    public void shouldToggleFollowingOnRemoval() throws CreateModelException {
        ops.removeFollowing(user).subscribe(observer);
        verify(followStatus).toggleFollowing(user.getId());
        verify(observer).onNext(false);
    }

    @Test
    public void shouldSyncFollowingsOnRemoval() {
        ops.removeFollowing(user).subscribe(observer);
        verify(syncInitiator).pushFollowingsToApi();
    }

    @Test
    public void shouldUpdateCacheForEachUserOnAddition() throws CreateModelException {
        ops.addFollowing(user);
        verify(scModelManager, times(1)).getCachedUser(user.getId());
    }

    @Test
    public void shouldUpdateCacheForEachUserOnRemoval() throws CreateModelException {
        ops.removeFollowing(user);
        verify(scModelManager, times(1)).getCachedUser(user.getId());
    }

    @Test
    public void shouldForceStreamToStaleIfFirstFollowingFromAddition() {
        TestObservables.MockObservable<Boolean> observable = TestObservables.emptyObservable();
        when(syncStateManager.forceToStaleAsync(Content.ME_SOUND_STREAM)).thenReturn(observable);
        when(followStatus.isEmpty()).thenReturn(true, false);
        ops.addFollowing(user);
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldNotForceStreamToStaleIfFollowingsNotEmpty() {
        when(followStatus.isEmpty()).thenReturn(false);
        ops.addFollowing(user);
        verify(syncStateManager, never()).forceToStale(Content.ME_SOUND_STREAM);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnAddition() throws CreateModelException {
        ops.addFollowing(user);
        verify(userAssociationStorage).follow(user);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnRemoval() throws CreateModelException {
        ops.removeFollowing(user);
        verify(userAssociationStorage).unfollow(user);
    }

    @Test
    public void shouldReturnTrueIfNoAssociationHasToken(){
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        verify(observer).onCompleted();
        verifyZeroInteractions(soundCloudRxHttpClient);
        verifyZeroInteractions(userAssociationStorage);
    }

    @Test
    public void shouldMakeAPostRequestWhenBulkFollowing() {
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationOne.getToken()).thenReturn("token1");
        when(userAssociationOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("POST");
    }

    @Test
    public void shouldMakeRequestToPublicAPIWhenBulkFollowing() {
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationOne.getToken()).thenReturn("token1");
        when(userAssociationOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddTokensAsQueryParametersWhenBulkFollowing() {
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationOne.getToken()).thenReturn("token1");
        when(userAssociationTwo.getToken()).thenReturn("token2");
        when(userAssociationOne.hasToken()).thenReturn(true);
        when(userAssociationTwo.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        Object jsonContent = argumentCaptor.getValue().getContent();
        expect(((FollowingOperations.BulkFollowingsHolder) jsonContent).tokens).toContainExactly("token1", "token2");
    }

    @Test
    public void shouldReturnTrueIfBulkFollowingRequestSucceeds(){
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationOne.getToken()).thenReturn("token1");
        when(userAssociationOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void bulkFollowingShouldCompleteImmediatelyIfTokenSetIsEmpty() {
        Collection<UserAssociation> noTokenAssociations = ModelFixtures.createDirtyFollowings(3);
        ops.bulkFollowAssociations(noTokenAssociations).subscribe(observer);
        verify(observer).onCompleted();
        verify(observer, never()).onNext(any(Collection.class));
    }

    @Test
    public void bulkFollowingShouldMarkSuggestedUserAssociationsAsSyncedWhenRequestSucceeds() {
        final UserAssociation association = new UserAssociation(Association.Type.FOLLOWING, user);
        association.markForAddition("123");
        final Set<UserAssociation> associations = Collections.singleton(association);

        APIResponse successResponse = mock(APIResponse.class);
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.just(successResponse));

        ops.bulkFollowAssociations(associations).subscribe(observer);
        verify(userAssociationStorage).setFollowingsAsSynced(associations);
    }

}
