package com.soundcloud.android.screens;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.main.NavigationDrawerFragment;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class MenuScreen {
    private Han solo;
    private Waiter waiter;

    private static int explore_selector = R.string.side_menu_explore;

    public MenuScreen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void logout() {
        solo.clickOnActionBarItem(R.id.action_settings);
        solo.clickOnText(R.string.pref_revoke_access);
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnOK();
        solo.waitForActivity(OnboardActivity.class);
        solo.waitForViewId(R.id.tour_bottom_bar, 5000);
    }

    public ListView rootMenu() {
        return (ListView) solo.waitForViewId(R.id.nav_drawer_listview, 20000);
    }

    public View youMenu() {
        return rootMenu().getChildAt(NavigationDrawerFragment.NavItem.PROFILE.ordinal());
    }

    public View clickHomeButton() {
        solo.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                solo.clickOnActionBarHomeButton();
            }
        });
        solo.sleep(1000);
        return youMenu();
    }

    public String getUserName() {
        TextView you = (TextView) youMenu().findViewById(R.id.username);
        return you.getText().toString();
    }

    public void openExplore() {
        solo.clickOnActionBarHomeButton();
        waiter.waitForDrawerToOpen();
        solo.clickOnText(explore_selector);
        waiter.waitForViewId(R.id.suggested_tracks_categories_list);
        waiter.waitForListContent();
    }


}
