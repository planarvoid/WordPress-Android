package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.Screen;

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
        GridView gridView = testDriver.getCurrentGridView();
        if (gridView == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridView.getChildAt(index);
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        testDriver.wrap(textView).click();
        return textView.getText().toString();

    }

    public String followRandomUser(){
        waitForUsers();
        GridView gridView = testDriver.getCurrentGridView();
        if (gridView == null) {
            throw new RuntimeException("No Gridview present when trying to follow random user");
        }
        ViewGroup viewGroup = (ViewGroup) gridView.getChildAt((int) (Math.random() * gridView.getChildCount() -1));
        TextView textView = (TextView) viewGroup.findViewById(R.id.username);
        testDriver.wrap(textView).click();
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

    public SuggestedUsersCategoryScreen waitForUsers() {
        waiter.waitForElement(R.id.suggested_users_grid);
        waiter.waitForElement(R.id.username);
        return this;
    }

    private GridView suggestedUsersGrid(){
        return testDriver.getCurrentGridView();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
