package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.associations.FollowingOperations.FollowStatusChangedListener;
import static com.soundcloud.android.view.adapters.UserItemPresenter.OnToggleFollowListener;
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
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class UserAdapterTest {

    private UserAdapter adapter;

    @Mock private UserItemPresenter userPresenter;
    @Mock private FollowingOperations followingOperations;
    @Mock private ViewGroup itemView;

    @Captor private ArgumentCaptor<List<PropertySet>> propSetCaptor;

    @Before
    public void setup() {
        adapter = new UserAdapter(Content.ME_FOLLOWINGS.uri, userPresenter, followingOperations);
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

        verify(userPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void convertsUserToPropertySet() throws CreateModelException {
        PublicApiUser user = createUser();
        when(followingOperations.isFollowing(user.getUrn())).thenReturn(true);
        adapter.addItems(Arrays.<PublicApiResource>asList(user));
        adapter.bindRow(0, itemView);

        verify(userPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedTrack = propSetCaptor.getValue().get(0);
        expect(convertedTrack.get(UserProperty.URN)).toEqual(user.getUrn());
        expect(convertedTrack.get(UserProperty.USERNAME)).toEqual(user.getUsername());
        expect(convertedTrack.get(UserProperty.FOLLOWERS_COUNT)).toEqual(user.followers_count);
        expect(convertedTrack.get(UserProperty.IS_FOLLOWED_BY_ME)).toEqual(true);
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

        verify(userPresenter, times(2)).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedTrack = propSetCaptor.getAllValues().get(1).get(0);
        expect(convertedTrack.get(UserProperty.URN)).toEqual(user2.getUrn());
    }

    @Test
    public void itemClickStartsProfileActivityWithUserArgument() throws CreateModelException {
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getName());
        expect(intent.getParcelableExtra(ProfileActivity.EXTRA_USER)).toBe(user);
    }

    @Test
    public void toggleFollowingSubscribesToFollowObservable() throws CreateModelException {
        PublicApiUser user = createUser();
        adapter.addItems(Arrays.<PublicApiResource>asList(user));

        final TestObservables.MockObservable<Boolean> observable = TestObservables.emptyObservable();
        when(followingOperations.toggleFollowing(user)).thenReturn(observable);

        ArgumentCaptor<OnToggleFollowListener> captor = ArgumentCaptor.forClass(OnToggleFollowListener.class);
        verify(userPresenter).setToggleFollowListener(captor.capture());

        captor.getValue().onToggleFollowClicked(0, Mockito.mock(ToggleButton.class));
        expect(observable.subscribedTo()).toBeTrue();
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

        verify(userPresenter).bindItemView(eq(1), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedUser = propSetCaptor.getValue().get(1);
        expect(convertedUser.get(UserProperty.URN)).toEqual(user2AfterUpdate.getUrn());
        expect(convertedUser.get(UserProperty.USERNAME)).toEqual(user2AfterUpdate.getUsername());
        expect(convertedUser.get(UserProperty.FOLLOWERS_COUNT)).toEqual(user2AfterUpdate.followers_count);
        expect(convertedUser.get(UserProperty.IS_FOLLOWED_BY_ME)).toEqual(true);
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

        verify(userPresenter).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        expect(propSetCaptor.getValue().get(0).get(UserProperty.URN)).toEqual(user.getUrn());
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