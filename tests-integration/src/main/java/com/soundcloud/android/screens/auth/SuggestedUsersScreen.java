package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.screens.EmailConfirmScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

import android.R.id;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SuggestedUsersScreen extends Screen {
    public static final Class ACTIVITY = SuggestedUsersActivity.class;

    public SuggestedUsersScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    public boolean hasContent(){
        testDriver.scrollListToTop(0);
        return testDriver.getView(R.id.list_section_header) != null;
    }

    public boolean hasFacebookSection(){
        testDriver.scrollListToTop(0);
        return testDriver.searchText(testDriver.getString(R.string.suggested_users_section_facebook), true);
    }

    public boolean hasMusicSection(){
        testDriver.scrollListToTop(0);
        return testDriver.searchText(testDriver.getString(R.string.suggested_users_section_music), true);
    }

    public boolean hasAudioSection(){
        testDriver.scrollListToTop(0);
        return testDriver.searchText(testDriver.getString(R.string.suggested_users_section_audio), true);
    }

    public SuggestedUsersCategoryScreen rockOut(){
        // TODO : do not click on text, but rather listview items
        testDriver.scrollListToTop(0);
        testDriver.clickOnText("Rock");
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    public String subtextAtIndexEquals(int index) {
        View categoryRow = getCategoryRow(index);
        return ((TextView) categoryRow.findViewById(android.R.id.text2)).getText().toString();
    }

    public SuggestedUsersScreen clickToggleCategoryCheckmark(int visibleIndex){
        clickOnCategoryElement(visibleIndex, R.id.btn_user_bucket_select_all);
        return new SuggestedUsersScreen(testDriver);
    }

    public SuggestedUsersCategoryScreen clickCategory(int visibleIndex) {
        clickOnCategoryElement(visibleIndex, android.R.id.text1);
        testDriver.waitForActivity(SuggestedUsersCategoryActivity.class);
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    public SuggestedUsersCategoryScreen goToFacebook() {
        testDriver.clickOnText("Facebook");
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    private void clickOnCategoryElement(int index, int elementId) {
        testDriver.findElement(With.id(android.R.id.list)).toListView().getItemAt(index).findElement(With.id(elementId)).click();
    }

    private View getCategoryRow(int index) {
        return ((ViewGroup) testDriver.getView(android.R.id.list)).getChildAt(index);
    }

    public EmailConfirmScreen finish() {
        testDriver.clickOnActionBarItem(R.id.finish);
        return new EmailConfirmScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
