package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.GridView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoryFragmentTest {

    private SuggestedUsersCategoryFragment fragment;

    @Mock private FollowingOperations followingOperations;
    @Mock private View fragmentView;
    @Mock private GridView gridView;

    private List<SuggestedUser> suggestedUsers;

    @Before
    public void setup() throws CreateModelException {
        when(fragmentView.findViewById(R.id.suggested_users_grid)).thenReturn(gridView);

        Category category = new Category();
        suggestedUsers = ModelFixtures.create(SuggestedUser.class, 3);
        category.setUsers(suggestedUsers);

        final Bundle args = new Bundle();
        args.putParcelable(Category.EXTRA, category);

        fragment = new SuggestedUsersCategoryFragment(followingOperations);
        Robolectric.shadowOf(fragment).setActivity(new FragmentActivity());
        Robolectric.shadowOf(fragment).setAttached(true);

        fragment.setArguments(args);
        fragment.onCreate(null);

        final List<Long> followings = Arrays.asList(suggestedUsers.get(0).getId(), suggestedUsers.get(2).getId());
        when(followingOperations.getFollowedUserIds()).thenReturn(new HashSet<>(followings));
        fragment.onViewCreated(fragmentView, null);
    }

    @Test
    public void shouldCheckFollowings() {
        verify(gridView).setItemChecked(0, true);
        verify(gridView).setItemChecked(1, false);
        verify(gridView).setItemChecked(2, true);
    }

    @Test
    public void shouldCallToggleFollowingBySuggestedUser() {
        when(followingOperations.toggleFollowingBySuggestedUser(any(SuggestedUser.class))).thenReturn(Observable.<Void>empty());
        fragment.onItemClick(null, null, 2, 0);
        verify(followingOperations).toggleFollowingBySuggestedUser(suggestedUsers.get(2));
    }

    @Test
    public void shouldFollowAllUsers() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(followingOperations.addFollowingsBySuggestedUsers(newArrayList(suggestedUsers.get(1)))).thenReturn(observable);
        fragment.toggleFollowings(true);

        verify(gridView, times(2)).setItemChecked(0, true);
        verify(gridView).setItemChecked(1, true);
        verify(gridView, times(2)).setItemChecked(2, true);

        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldUnfollowAllUsers(){
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(followingOperations.removeFollowingsBySuggestedUsers(newArrayList(suggestedUsers.get(0), suggestedUsers.get(2)))).thenReturn(observable);
        fragment.toggleFollowings(false);

        verify(gridView).setItemChecked(0, false);
        verify(gridView, times(2)).setItemChecked(1, false);
        verify(gridView).setItemChecked(2, false);

        expect(observable.subscribedTo()).toBeTrue();
    }

}
