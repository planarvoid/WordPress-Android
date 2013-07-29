package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SuggestedUsersScreen {
    private final Waiter waiter;
    private Han solo;

    public SuggestedUsersScreen(Han driver) {
        solo = driver;
        waiter = new Waiter(solo);
    }

    public boolean hasContent(){
        solo.scrollListToTop(0);
        return solo.getView(R.id.suggested_users_list_header) != null;
    }

    public boolean hasFacebookSection(){
        solo.scrollListToTop(0);
        return solo.searchText(solo.getString(R.string.suggested_users_section_facebook), true);
    }

    public boolean hasMusicSection(){
        solo.scrollListToTop(0);
        return solo.searchText(solo.getString(R.string.suggested_users_section_music), true);
    }

    public boolean hasAudioSection(){
        solo.scrollListToTop(0);
        return solo.searchText(solo.getString(R.string.suggested_users_section_audio), true);
    }

    public void rockOut(){
        // TODO : do not click on text, but rather listview items
        solo.scrollListToTop(0);
        solo.clickOnText("Rock");
        solo.waitForActivity(SuggestedUsersCategoryActivity.class);
    }

    public String subtextAtIndexEquals(int index) {
        View categoryRow = getCategoryRow(index);
        return ((TextView) categoryRow.findViewById(android.R.id.text2)).getText().toString();
    }

    public void clickToggleCategoryCheckmark(int visibleIndex){
        clickOnCategoryElement(visibleIndex, R.id.btn_user_bucket_select_all);
    }

    public void clickCategory(int visibleIndex) {
        clickOnCategoryElement(visibleIndex, android.R.id.text1);
        solo.waitForActivity(SuggestedUsersCategoryActivity.class);
    }

    public void goToFacebook() {
        solo.clickOnText("Facebook");
        solo.waitForActivity(SuggestedUsersCategoryActivity.class);
    }

    private void clickOnCategoryElement(int index, int elementId) {
        View categoryRow = getCategoryRow(index);
        solo.clickOnView(categoryRow.findViewById(elementId));
    }

    private View getCategoryRow(int index) {
        // wait for list items
        solo.waitForViewId(R.id.btn_user_bucket_select_all, 5000);
        return ((ViewGroup) solo.getView(android.R.id.list)).getChildAt(index);
    }

    public HomeScreen finish() {
        solo.clickOnView(R.id.finish);
        solo.waitForActivity(Home.class, 30000);
        waiter.waitForListContent();
        return new HomeScreen(solo);
    }
}
