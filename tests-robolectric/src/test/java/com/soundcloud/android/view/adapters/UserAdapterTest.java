package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.associations.FollowingOperations.FollowStatusChangedListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserItem;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Intent;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class UserAdapterTest {

    private UserAdapter adapter;

    @Mock private UserItemRenderer userRenderer;
    @Mock private FollowingOperations followingOperations;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<UserItem>> itemCaptor;

    @Before
    public void setup() {
        adapter = new UserAdapter(Content.ME_FOLLOWINGS.uri, userRenderer, followingOperations);
    }

    @Test
    public void constructorRequestsUserFollowings() {
        verify(followingOperations).requestUserFollowings(any(FollowStatusChangedListener.class));
    }

    @Test
    public void bindsUserToRowViaPresenter() throws CreateModelException {
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user));

        adapter.bindRow(0, itemView);

        verify(userRenderer).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void convertsUserToPropertySet() throws CreateModelException {
        PublicApiUser user = createUser();
        when(followingOperations.isFollowing(user.getUrn())).thenReturn(true);
        adapter.addItems(Arrays.<PublicApiResource>asList(user));
        adapter.bindRow(0, itemView);

        verify(userRenderer).bindItemView(eq(0), refEq(itemView), itemCaptor.capture());
        UserItem userItem = itemCaptor.getValue().get(0);
        expect(userItem.getEntityUrn()).toEqual(user.getUrn());
        expect(userItem.getName()).toEqual(user.getUsername());
        expect(userItem.getFollowersCount()).toEqual(user.followers_count);
    }

    @Test
    public void clearItemsClearsInitialPropertySets() throws CreateModelException {
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user));
        adapter.bindRow(0, itemView);
        adapter.clearData();

        PublicApiUser user2 = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user2));
        adapter.bindRow(0, itemView);

        verify(userRenderer, times(2)).bindItemView(eq(0), refEq(itemView), itemCaptor.capture());
        UserItem userItem = itemCaptor.getAllValues().get(1).get(0);
        expect(userItem.getEntityUrn()).toEqual(user2.getUrn());
    }

    @Test
    public void itemClickStartsProfileActivityWithUserArgument() throws CreateModelException {
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES, null);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getName());
        expect(intent.getParcelableExtra(ProfileActivity.EXTRA_USER)).toBe(user);
    }

    @Test
    public void updateItemsReplacesCurrentItem() throws CreateModelException {
        PublicApiUser user = createUser();
        PublicApiUser user2 = createUser();
        when(followingOperations.isFollowing(user2.getUrn())).thenReturn(true);
        adapter.addItems(Arrays.<PublicApiResource>asList(user, user2));

        PublicApiUser user2AfterUpdate = copyUser(user2);
        final HashMap<Urn, PublicApiResource> updatedItems = Maps.newHashMap();
        updatedItems.put(user2AfterUpdate.getUrn(), user2AfterUpdate);
        adapter.updateItems(updatedItems);

        expect(adapter.getCount()).toBe(2);

        adapter.bindRow(1, itemView);

        verify(userRenderer).bindItemView(eq(1), refEq(itemView), itemCaptor.capture());
        UserItem userItem = itemCaptor.getValue().get(1);
        expect(userItem.getEntityUrn()).toEqual(user2AfterUpdate.getUrn());
        expect(userItem.getName()).toEqual(user2AfterUpdate.getUsername());
        expect(userItem.getFollowersCount()).toEqual(user2AfterUpdate.followers_count);
    }

    @Test
    public void addItemsFiltersOutUsersWithoutUsernames() throws CreateModelException {
        PublicApiUser userWithoutUsername = createUser();
        userWithoutUsername.setUsername("");
        PublicApiUser user = createUser();

        adapter.addItems(Arrays.<PublicApiResource>asList(user, userWithoutUsername));

        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void bindsUserItemsAfterFilteringUsersWithoutUsernames() throws CreateModelException {
        PublicApiUser userWithoutUsername = createUser();
        userWithoutUsername.setUsername("");
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(userWithoutUsername, user));

        adapter.bindRow(0, itemView);

        verify(userRenderer).bindItemView(eq(0), refEq(itemView), itemCaptor.capture());
        expect(itemCaptor.getValue().get(0).getEntityUrn()).toEqual(user.getUrn());
    }

    private PublicApiUser copyUser(PublicApiUser user) throws CreateModelException {
        final PublicApiUser userNewInstance = createUser();
        userNewInstance.setUrn(user.getUrn().toString());
        return userNewInstance;
    }

    private PublicApiUser createUser() throws CreateModelException {
        return ModelFixtures.create(PublicApiUser.class);
    }
}