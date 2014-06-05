package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.view.GridViewCompat;

import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SuggestedUsersCategoryScreen extends Screen {
    private static final Class ACTIVITY = SuggestedUsersCategoryActivity.class;

    public SuggestedUsersCategoryScreen(Han solo) {
        super(solo);
    }

    public String followUser(int index) {
        waitForUsers();
        GridViewCompat gridViewCompat = (GridViewCompat) testDriver.getCurrentGridView();
        if (gridViewCompat == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridViewCompat.getChildAt(index);
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        testDriver.clickOnView(textView);
        return textView.getText().toString();

    }

    public String followRandomUser(){
        waitForUsers();
        GridViewCompat gridViewCompat = (GridViewCompat) testDriver.getCurrentGridView();
        if (gridViewCompat == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridViewCompat.getChildAt((int) (Math.random() * gridViewCompat.getChildCount()));
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        testDriver.clickOnView(textView);
        return textView.getText().toString();
    }

    public boolean hasAllUsersSelected() {
        return testAll() == true;
    }

    public boolean hasNoUsersSelected() {
        return testAll() == false;
    }

    public void selectAll() {
        testDriver.clickOnActionBarItem(R.id.menu_select_all);
    }

    public void deselectAll() {
        testDriver.clickOnActionBarItem(R.id.menu_deselect_all);
    }

    private boolean testAll(){
        waitForUsers();
        GridView list = suggestedUsersGrid();

        for (int i = 0; i < list.getCount(); i++) {
            ToggleButton button = (ToggleButton) list.getChildAt(0).findViewById(R.id.toggle_btn_follow);
            if (button.isChecked()) {
                return true;
            }
        }
        return false;
    }

    public void waitForUsers() {
        testDriver.waitForActivity(SuggestedUsersCategoryActivity.class);
        testDriver.waitForViewId(R.id.suggested_users_grid, 10000);
        testDriver.waitForViewId(R.id.username, 10000);
    }

    private GridView suggestedUsersGrid(){
        return testDriver.getCurrentGridView();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
