package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.view.MainMenu;

import android.view.View;
import android.widget.TextView;

public class MenuScreen {
    private Han solo;
    private Waiter waiter;

    public MenuScreen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public void logout() {
        solo.clickOnView(R.id.custom_home);
        solo.clickOnMenuItem(R.string.side_menu_settings);
        solo.clickOnText(R.string.pref_revoke_access);
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnOK();
        solo.waitForActivity(Onboard.class);
        solo.waitForViewId(R.id.tour_bottom_bar, 5000);
    }

    public MainMenu rootMenu() {
        return (MainMenu) solo.waitForViewId(R.id.root_menu, 20000);
    }

    public View youMenu() {
        return rootMenu().findViewById(R.id.nav_you);
    }

    public String getUserName() {
        TextView you = (TextView) youMenu().findViewById(R.id.main_menu_item_text);
        return you.getText().toString();
    }

    public void openExplore() {
        clickSideMenuOption(R.id.nav_explore, R.id.pager);
    }

    public void openStream() {
        clickSideMenuOption(R.id.nav_stream, R.id.content_frame);
    }

    private void clickSideMenuOption(int menuId, int viewToWaitForId){
        solo.clickOnView(R.id.custom_home);
        View exploreMenuButton = rootMenu().findViewById(menuId);
        solo.clickOnView(exploreMenuButton);
        solo.waitForViewId(viewToWaitForId, 3000);
        waiter.waitForListContent();
    }
}
