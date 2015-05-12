package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.Screen;

import android.R.id;

public class SuggestedUsersScreen extends Screen {
    public static final Class ACTIVITY = SuggestedUsersActivity.class;

    public SuggestedUsersScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
        waiter.waitForElement(id.content);
    }

    public boolean hasContent(){
        testDriver.scrollListToTop(0);
        return testDriver.findElement(With.id(R.id.list_section_header)).isVisible();
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
        testDriver.findElement(With.text("Rock")).click();
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    public String getSubtext(ViewElement view) {
        return new TextElement(view.findElement(With.id(id.text2))).getText();
    }

    public String getSubtextAtIndex(int index) {
        ViewElement categoryRow = getCategoryRow(index);
        return getSubtext(categoryRow);
    }

    public SuggestedUsersScreen clickToggleCategoryCheckmark(int visibleIndex){
        clickOnCategoryElement(visibleIndex, R.id.btn_user_bucket_select_all);
        return this;
    }

    public SuggestedUsersCategoryScreen clickCategory(int visibleIndex) {
        clickOnCategoryElement(visibleIndex, id.text1);
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    public SuggestedUsersCategoryScreen goToFacebook() {
        testDriver.findElement(With.text("Facebook Friends")).click();
        return new SuggestedUsersCategoryScreen(testDriver);
    }

    private void clickOnCategoryElement(int index, int elementId) {
        getCategoryRow(index).findElement(With.id(elementId)).click();
    }

    private ViewElement getCategoryRow(int index) {
        return testDriver.findElement(With.id(id.list)).toListView().getItemAt(index);
    }

    public EmailOptInScreen finish() {
        testDriver.clickOnActionBarItem(R.id.finish);
        return new EmailOptInScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
