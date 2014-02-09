package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.GridViewCompat;

import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Set;

public class SuggestedUsersCategoryScreen extends Screen {
    private static final Class ACTIVITY = SuggestedUsersCategoryActivity.class;

    public SuggestedUsersCategoryScreen(Han solo) {
        super(solo);
    }

    public String followUser(int index) {
        waitForUsers();
        GridViewCompat gridViewCompat = (GridViewCompat) solo.getCurrentGridView();
        if (gridViewCompat == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridViewCompat.getChildAt(index);
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        solo.clickOnView(textView);
        return textView.getText().toString();

    }

    public String followRandomUser(){
        waitForUsers();
        GridViewCompat gridViewCompat = (GridViewCompat) solo.getCurrentGridView();
        if (gridViewCompat == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridViewCompat.getChildAt((int) (Math.random() * gridViewCompat.getChildCount()));
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        solo.clickOnView(textView);
        return textView.getText().toString();
    }

    public boolean hasAllUsersSelected() {
        return testAll(true);
    }

    public boolean hasNoUsersSelected() {
        return testAll(false);
    }

    public void selectAll() {
        solo.clickOnActionBarItem(R.id.menu_select_all);
    }

    public void deselectAll() {
        solo.clickOnActionBarItem(R.id.menu_deselect_all);
    }

    private boolean testAll(boolean isFollowing){
        waitForUsers();
        ListAdapter adapter = suggestedUsersGrid().getAdapter();
        Set<Long> followingIds = new FollowingOperations().getFollowedUserIds();
        for (int i = 0; i < adapter.getCount(); i++){
            final boolean actual = followingIds.contains(((SuggestedUser) adapter.getItem(i)).getId());
            if ((isFollowing && !actual) || (!isFollowing && actual)){
                return false;
            }
        }
        return true;
    }

    public void waitForUsers() {
        solo.waitForActivity(SuggestedUsersCategoryActivity.class);
        solo.waitForViewId(R.id.suggested_users_grid, 10000);
        solo.waitForViewId(R.id.username, 10000);
    }

    private GridView suggestedUsersGrid(){
        return solo.getCurrentGridView();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
