package com.soundcloud.android.fragment;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.view.GridViewCompat;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;

import android.os.Bundle;
import android.view.View;

import java.util.HashSet;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersCategoryFragmentTest {

    private SuggestedUsersCategoryFragment fragment;
    @Mock
    private FollowingOperations followingOperations;
    @Mock
    private View fragmentView;
    @Mock
    private GridViewCompat gridView;
    @Mock
    private Observable observable;

    private List<SuggestedUser> suggestedUsers;

    @Before
    public void setup() throws CreateModelException {

        when(followingOperations.observeOn(any(Scheduler.class))).thenReturn(followingOperations);
        when(fragmentView.findViewById(R.id.gridview)).thenReturn(gridView);

        Category category = new Category("category1");
        suggestedUsers = TestHelper.createSuggestedUsers(3);
        category.setUsers(suggestedUsers);

        final Bundle args = new Bundle();
        args.putParcelable(Category.EXTRA, category);

        fragment = new SuggestedUsersCategoryFragment(followingOperations);
        fragment.setArguments(args);
        fragment.onCreate(null);

        final HashSet<Long> followingSet = Sets.newHashSet(suggestedUsers.get(0).getId(), suggestedUsers.get(2).getId());
        when(followingOperations.getFollowedUserIds()).thenReturn(followingSet);
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
    public void shouldFollowAllUsers(){
        when(followingOperations.addFollowingsBySuggestedUsers(Lists.newArrayList(suggestedUsers.get(1)))).thenReturn(observable);
        fragment.toggleFollowings(true);

        verify(observable).subscribe(any(Observer.class));
        verify(gridView, times(2)).setItemChecked(0, true);
        verify(gridView).setItemChecked(1, true);
        verify(gridView, times(2)).setItemChecked(2, true);
    }

    @Test
    public void shouldUnfollowAllUsers(){
        when(followingOperations.removeFollowingsBySuggestedUsers(Lists.newArrayList(suggestedUsers.get(0), suggestedUsers.get(2)))).thenReturn(observable);
        fragment.toggleFollowings(false);

        verify(observable).subscribe(any(Observer.class));
        verify(gridView).setItemChecked(0, false);
        verify(gridView, times(2)).setItemChecked(1, false);
        verify(gridView).setItemChecked(2, false);
    }

}
