package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class SuggestedUsersScreen {
    private final Waiter waiter;
    private Han solo;

    public SuggestedUsersScreen(Han driver) {
        solo = driver;
        waiter = new Waiter(solo);
    }

    public boolean hasContent(){
        return solo.getView(R.id.suggested_users_list_header) != null;
    }

    public boolean hasFacebookSection(){
        return solo.searchText(solo.getString(R.string.suggested_users_section_facebook), true);
    }

    public boolean hasMusicSection(){
        return solo.searchText(solo.getString(R.string.suggested_users_section_music), true);
    }

    public boolean hasAudioSection(){
        return solo.searchText(solo.getString(R.string.suggested_users_section_audio), true);
    }

    public void rockOut(){
        // TODO : do not click on text, but rather listview items
        solo.scrollListToTop(0);
        solo.clickOnText("Rock");
        solo.waitForActivity(SuggestedUsersCategoryActivity.class);
    }

    public HomeScreen finish() {
        solo.clickOnActionBarItem(R.id.finish);
        solo.waitForActivity(Home.class);
        waiter.waitForListContent();
        return new HomeScreen(solo);
    }
}
