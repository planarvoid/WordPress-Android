package com.soundcloud.android.operations.following;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.service.sync.SyncStateManager;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class FollowingOperationsTest {

    private FollowingOperations ops;
    private UserAssociationStorage userAssociationStorage;
    private FollowStatus followStatus;
    private SyncStateManager syncStateManager;
    private ScModelManager scModelManager;

    private User user;
    private List<User> users;
    private SuggestedUser suggestedUser;
    private List<SuggestedUser> suggestedUsers;

    @Mock
    Observable observable;

    @Before
    public void before() throws CreateModelException {
        userAssociationStorage = mock(UserAssociationStorage.class);
        followStatus = mock(FollowStatus.class);
        syncStateManager = mock(SyncStateManager.class);
        scModelManager = mock(ScModelManager.class);
        when(scModelManager.cache(any(User.class), any(ScResource.CacheUpdateMode.class))).thenReturn(mock(User.class));

        ops = new FollowingOperations(userAssociationStorage, followStatus, syncStateManager, scModelManager);

        user = TestHelper.getModelFactory().createModel(User.class);
        users = TestHelper.createUsers(5);

        suggestedUser = TestHelper.getModelFactory().createModel(SuggestedUser.class);
        suggestedUsers = TestHelper.createSuggestedUsers(3);
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
}
