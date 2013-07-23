package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.GridViewCompat;

import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Set;

public class SuggestedUsersCategoryScreen {

    private Han solo;

    public SuggestedUsersCategoryScreen(Han driver) {
        solo = driver;
    }

    public String followRandomUser(){
        waitForUsers();
        GridViewCompat gridViewCompat = (GridViewCompat) solo.getCurrentGridView();
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
        solo.clickOnView(R.id.menu_select_all);
    }

    public void deselectAll() {
        solo.clickOnView(R.id.menu_deselect_all);
    }

    private boolean testAll(boolean isFollowing){
        waitForUsers();

        ListAdapter adapter = solo.getCurrentGridView().getAdapter();
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
        solo.waitForViewId(R.id.username, 5000);
    }
}
