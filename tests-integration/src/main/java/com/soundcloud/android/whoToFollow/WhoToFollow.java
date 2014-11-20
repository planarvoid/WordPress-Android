package com.soundcloud.android.whoToFollow;


import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class WhoToFollow extends ActivityTestCase<OnboardActivity> {

    private Waiter waiter;
    protected HomeScreen homeScreen;
    protected SignUpScreen signUpScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;

    public WhoToFollow() {
        super(OnboardActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        signUpScreen  = new SignUpScreen(solo);
        waiter = new Waiter(solo);
    }

    public void testCheckmarkSelection() {
        createNewUser();
        waiter.waitForContentAndRetryIfLoadingFailed();

        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersCategoryScreen = suggestedUsersScreen.clickCategory(1);
        assertEquals("All users should be selected", true, suggestedUsersCategoryScreen.hasAllUsersSelected());

        solo.goBack();
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertEquals("Users should not be selected", true, suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    public void testIndividualUserSelection() {
        createNewUser();
        suggestedUsersCategoryScreen = suggestedUsersScreen.clickCategory(1);
        String followed = suggestedUsersCategoryScreen.followUser(2);
        solo.goBack();
        assertEquals(followed, suggestedUsersScreen.getSubtextAtIndex(1));
    }

    public void testSelectDeselectToggle() {
        createNewUser();
        suggestedUsersCategoryScreen = suggestedUsersScreen.clickCategory(1);
        suggestedUsersCategoryScreen.waitForUsers();
        suggestedUsersCategoryScreen.selectAll();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
        suggestedUsersCategoryScreen.deselectAll();
        assertTrue(suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected String generateEmail() {
        return "slawomir-"+System.currentTimeMillis()+"@tests.soundcloud";
    }

    private void createNewUser() {
        homeScreen = new HomeScreen(solo);
        homeScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        suggestedUsersScreen = signUpScreen.waitForSuggestedUsers();
    }

}
