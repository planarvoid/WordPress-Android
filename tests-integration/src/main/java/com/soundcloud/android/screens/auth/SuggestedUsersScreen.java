package com.soundcloud.android.screens.auth;

import android.R.id;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

public class SuggestedUsersScreen extends Screen {
    public static final Class ACTIVITY = SuggestedUsersActivity.class;

    public SuggestedUsersScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    public boolean hasContent(){
        solo.scrollListToTop(0);
        return solo.getView(R.id.list_section_header) != null;
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

    public SuggestedUsersCategoryScreen rockOut(){
        // TODO : do not click on text, but rather listview items
        solo.scrollListToTop(0);
        solo.clickOnText("Rock");
        return new SuggestedUsersCategoryScreen(solo);
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

    public SuggestedUsersCategoryScreen goToFacebook() {
        solo.clickOnText("Facebook");
        return new SuggestedUsersCategoryScreen(solo);
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

    //TODO: Investigate why does it take 60 seconds to show the stream
    public HomeScreen finish() {
        solo.clickOnActionBarItem(R.id.finish);
        solo.waitForActivity(MainActivity.class, 60000);
        waiter.waitForListContentAndRetryIfLoadingFailed();
        return new HomeScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
