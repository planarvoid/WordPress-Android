package com.soundcloud.android.whoToFollow;

import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.screens.auth.OnboardScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class WhoToFollow extends ActivityTestCase<Onboard> {

    private Waiter waiter;
    protected OnboardScreen onboardScreen;
    protected SignUpScreen signUpScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;

    public WhoToFollow() {
        super(Onboard.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        suggestedUsersScreen = new SuggestedUsersScreen(solo);
        suggestedUsersCategoryScreen = new SuggestedUsersCategoryScreen(solo);
        onboardScreen = new OnboardScreen(solo);
        signUpScreen  = new SignUpScreen(solo);
        waiter = new Waiter(solo);
    }

    public void testCheckmarkSelection() throws Exception {
        createNewUser();
        waiter.waitForListContent();

        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertEquals(true, suggestedUsersCategoryScreen.hasAllUsersSelected());

        solo.goBack();
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertEquals(true, suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    public void testIndividualUserSelection() throws Exception {
        createNewUser();
        suggestedUsersScreen.clickCategory(1);
        String followed = suggestedUsersCategoryScreen.followUser(2);
        solo.goBack();
        assertEquals(followed, suggestedUsersScreen.subtextAtIndexEquals(1));
    }

    public void testSelectDeselectToggle() throws Exception {
        createNewUser();
        suggestedUsersScreen.clickCategory(1);
        suggestedUsersCategoryScreen.waitForUsers();
        suggestedUsersCategoryScreen.selectAll();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
        suggestedUsersCategoryScreen.deselectAll();
        assertTrue(suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    @Override
    public void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }

    protected String generateEmail() {
        return "slawomir-"+System.currentTimeMillis()+"@tests.soundcloud";
    }

    private void createNewUser() {
        onboardScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        signUpScreen.waitForSuggestedUsers();
    }

}
