package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.fragment.NavigationDrawerFragment;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class MenuScreen {
    private Han solo;
    private Waiter waiter;

    public MenuScreen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void logout() {
        solo.clickOnView(R.id.home);
        solo.clickOnMenuItem(R.string.side_menu_settings);
        solo.clickOnText(R.string.pref_revoke_access);
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnOK();
        solo.waitForActivity(Onboard.class);
        solo.waitForViewId(R.id.tour_bottom_bar, 5000);
    }

    public ListView rootMenu() {
        return (ListView) solo.waitForViewId(R.id.nav_drawer_listview, 20000);
    }

    public View youMenu() {
        return rootMenu().getChildAt(NavigationDrawerFragment.NavItem.PROFILE.ordinal());
    }

    public String getUserName() {
        TextView you = (TextView) youMenu().findViewById(R.id.username);
        return you.getText().toString();
    }

    public void openExplore() {
        View item = rootMenu().getChildAt(NavigationDrawerFragment.NavItem.EXPLORE.ordinal());
        solo.clickOnView(R.id.home);
        solo.clickOnView(item);
        waiter.waitForListContent();
    }
}
