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
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserProperty;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.provider.Content;
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

    private TestEventBus eventBus = new TestEventBus();

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
        User user = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user));

        adapter.bindRow(0, itemView);

        verify(userPresenter).bindItemView(eq(0), refEq(itemView), anyList());
    }

    @Test
    public void convertsUserToPropertySet() throws CreateModelException {
        User user = TestHelper.getModelFactory().createModel(User.class);
        when(followingOperations.isFollowing(user.getUrn())).thenReturn(true);
        adapter.addItems(Arrays.<ScResource>asList(user));
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
        User user = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user));
        adapter.bindRow(0, itemView);
        adapter.clearData();

        User user2 = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user2));
        adapter.bindRow(0, itemView);

        verify(userPresenter, times(2)).bindItemView(eq(0), refEq(itemView), propSetCaptor.capture());
        PropertySet convertedTrack = propSetCaptor.getAllValues().get(1).get(0);
        expect(convertedTrack.get(UserProperty.URN)).toEqual(user2.getUrn());
    }

    @Test
    public void itemClickStartsProfileActivityWithUserArgument() throws CreateModelException {
        User user = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user));

        adapter.handleListItemClick(Robolectric.application, 0, 1L, Screen.YOUR_LIKES);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getName());
        expect(intent.getParcelableExtra(ProfileActivity.EXTRA_USER)).toBe(user);
    }

    @Test
    public void toggleFollowingSubscribesToFollowObservable() throws CreateModelException {
        User user = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user));

        final TestObservables.MockObservable<Boolean> observable = TestObservables.emptyObservable();
        when(followingOperations.toggleFollowing(user)).thenReturn(observable);

        ArgumentCaptor<OnToggleFollowListener> captor = ArgumentCaptor.forClass(OnToggleFollowListener.class);
        verify(userPresenter).setToggleFollowListener(captor.capture());

        captor.getValue().onToggleFollowClicked(0, Mockito.mock(ToggleButton.class));
        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void updateItemsReplacesCurrentItem() throws CreateModelException {
        User user = TestHelper.getModelFactory().createModel(User.class);
        User user2 = TestHelper.getModelFactory().createModel(User.class);
        adapter.addItems(Arrays.<ScResource>asList(user, user2));

        User user3 = TestHelper.getModelFactory().createModel(User.class);
        user3.setUrn(user2.getUrn().toString());

        final HashMap<Urn, ScResource> updatedItems = Maps.newHashMap();
        updatedItems.put(user3.getUrn(), user3);
        adapter.updateItems(updatedItems);

        expect(adapter.getItems()).toContainExactly(user, user3);
    }
}