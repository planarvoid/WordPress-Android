package com.soundcloud.android.operations.following;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.service.sync.SyncStateManager;
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
    private UserAssociation userAssociationsOne;
    @Mock
    private UserAssociation userAssociationsTwo;
    @Mock
    private Observable observable;
    @Mock
    private Observer<Collection<UserAssociation>> observer;

    private User user;
    private List<User> users;
    private SuggestedUser suggestedUser;
    private List<SuggestedUser> suggestedUsers;
    private Collection<UserAssociation> userAssociations;

    @Before
    public void before() throws CreateModelException {
        when(scModelManager.cache(any(User.class), any(ScResource.CacheUpdateMode.class))).thenReturn(mock(User.class));

        ops = new FollowingOperations(soundCloudRxHttpClient, userAssociationStorage, syncStateManager, followStatus, scModelManager);

        user = TestHelper.getModelFactory().createModel(User.class);
        users = TestHelper.createUsers(5);

        suggestedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        suggestedUsers = TestHelper.createSuggestedUsers(3);

        userAssociations = Lists.newArrayList(userAssociationsOne, userAssociationsTwo);
    }

    @Test
    public void shouldToggleFollowingOnAddition() throws CreateModelException {
        ops.addFollowing(user);
        verify(followStatus).toggleFollowing(user.getId());
    }

    @Test
    public void shouldToggleFollowingsOnAdditions() throws CreateModelException {
        ops.addFollowings(users);
        verify(followStatus).toggleFollowing(ScModel.getIdList(users));
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
        ops.removeFollowing(user);
        verify(followStatus).toggleFollowing(user.getId());
    }

    @Test
    public void shouldToggleFollowingsListOnRemoval() throws CreateModelException {
        ops.removeFollowings(users);
        verify(followStatus).toggleFollowing(ScModel.getIdList(users));
    }

    @Test
    public void shouldUpdateCacheForEachUserOnAddition() throws CreateModelException {
        ops.addFollowing(user);
        verify(scModelManager, times(1)).getCachedUser(user.getId());
    }

    @Test
    public void shouldUpdateCacheForEachUserOnListAddition() throws CreateModelException {
        ops.addFollowings(users);
        verify(scModelManager, times(5)).getCachedUser(anyLong());
    }

    @Test
    public void shouldUpdateCacheForEachUserOnRemoval() throws CreateModelException {
        ops.removeFollowing(user);
        verify(scModelManager, times(1)).getCachedUser(user.getId());
    }

    @Test
    public void shouldUpdateCacheForEachUserOnListRemoval() throws CreateModelException {
        ops.removeFollowings(users);
        verify(scModelManager, times(5)).getCachedUser(anyLong());
    }

    @Test
    public void shouldForceStreamToStaleIfFirstFollowingFromAddition() {
        when(syncStateManager.forceToStale(Content.ME_SOUND_STREAM)).thenReturn(observable);
        when(followStatus.isEmpty()).thenReturn(true, false);
        ops.addFollowing(user);
        verify(observable).subscribe(ScActions.NO_OP);
    }

    @Test
    public void shouldForceStreamToStaleIfFirstFollowingFromListAddition() {
        when(syncStateManager.forceToStale(Content.ME_SOUND_STREAM)).thenReturn(observable);
        when(followStatus.isEmpty()).thenReturn(true, false);
        ops.addFollowings(users);
        verify(observable).subscribe(ScActions.NO_OP);
    }

    @Test
    public void shouldNotForceStreamToStaleIfFollowingsNotEmpty() {
        when(followStatus.isEmpty()).thenReturn(false);
        ops.addFollowing(user);
        verify(syncStateManager, never()).forceToStale(Content.ME_SOUND_STREAM);
    }

    @Test
    public void shouldNotForceStreamToStaleFromListIfFollowingsNotEmpty() {
        when(followStatus.isEmpty()).thenReturn(false);
        ops.addFollowings(users);
        verify(syncStateManager, never()).forceToStale(Content.ME_SOUND_STREAM);
    }

    @Test
    public void shouldNotForceStreamToStaleFromListIfUsersListIsEmpty() {
        when(followStatus.isEmpty()).thenReturn(true);
        ops.addFollowings(Collections.<User>emptyList());
        verify(syncStateManager, never()).forceToStale(Content.ME_SOUND_STREAM);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnAddition() throws CreateModelException {
        ops.addFollowing(user);
        verify(userAssociationStorage).addFollowing(user);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnRemoval() throws CreateModelException {
        ops.removeFollowing(user);
        verify(userAssociationStorage).removeFollowing(user);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnListAddition() throws CreateModelException {
        ops.addFollowings(users);
        verify(userAssociationStorage).addFollowings(users);
    }

    @Test
    public void shouldCommitFollowingsListToLocalStorageOnListRemoval() throws CreateModelException {
        ops.removeFollowings(users);
        verify(userAssociationStorage).removeFollowings(users);
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
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("POST");
    }

    @Test
    public void shouldMakeRequestToPublicAPIWhenBulkFollowing() {
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddTokensAsQueryParametersWhenBulkFollowing() {
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsTwo.getToken()).thenReturn("token2");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        when(userAssociationsTwo.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchResponse(argumentCaptor.capture());
        Object jsonContent = argumentCaptor.getValue().getContent();
        expect(((FollowingOperations.BulkFollowingsHolder) jsonContent).tokens).toContainExactly("token1", "token2");
    }

    @Test
    public void shouldReturnTrueIfBulkFollowingRequestSucceeds(){
        when(soundCloudRxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());
        when(userAssociationsOne.getToken()).thenReturn("token1");
        when(userAssociationsOne.hasToken()).thenReturn(true);
        ops.bulkFollowAssociations(userAssociations).subscribe(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void bulkFollowingShouldCompleteImmediatelyIfTokenSetIsEmpty() {
        Collection<UserAssociation> noTokenAssociations = TestHelper.createDirtyFollowings(3);
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
