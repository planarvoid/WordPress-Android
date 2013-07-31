package com.soundcloud.android.whoToFollow;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.SuggestedUsersActivity;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class WhoToFollow extends ActivityTestCase<SuggestedUsersActivity> {

    private SuggestedUsersScreen suggestedUsersScreen;
    private SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;
    private Waiter waiter;

    public WhoToFollow() {
        super(SuggestedUsersActivity.class);
    }

    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();
        IntegrationTestHelper.unfollowAll();
        suggestedUsersScreen = new SuggestedUsersScreen(solo);
        suggestedUsersCategoryScreen = new SuggestedUsersCategoryScreen(solo);
        waiter = new Waiter(solo);
        solo.waitForActivity(SuggestedUsersActivity.class);
        waiter.waitForListContent();

    }

    public void testCheckmarkSelection() throws Exception {
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertEquals(true, suggestedUsersCategoryScreen.hasAllUsersSelected());

        solo.goBack();
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertEquals(true, suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    public void testIndividualUserSelection() throws Exception {
        suggestedUsersScreen.clickCategory(1);
        String followed = suggestedUsersCategoryScreen.followRandomUser();
        solo.goBack();
        assertEquals(followed, suggestedUsersScreen.subtextAtIndexEquals(1));
    }

    public void testSelectDeselectToggle() throws Exception {
        suggestedUsersScreen.clickCategory(1);
        suggestedUsersCategoryScreen.waitForUsers();
        suggestedUsersCategoryScreen.selectAll();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
        suggestedUsersCategoryScreen.deselectAll();
        assertTrue(suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    @Override
    public void tearDown() throws Exception {
        IntegrationTestHelper.unfollowAll();
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}
