package com.soundcloud.android.tests.whoToFollow;


import static com.soundcloud.android.framework.TestUser.generateEmail;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.SignUpBasicsScreen;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.ActivityTest;

public class WhoToFollowTest extends ActivityTest<OnboardActivity> {

    private Waiter waiter;
    protected HomeScreen homeScreen;
    protected SignUpMethodScreen signUpMethodScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;
    protected SignUpBasicsScreen signUpBasicsScreen;

    public WhoToFollowTest() {
        super(OnboardActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
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
        assertTrue("All users should be selected", suggestedUsersCategoryScreen.hasAllUsersSelected());

        suggestedUsersCategoryScreen.deselectAll();
        assertTrue("All users should not be selected", suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void createNewUser() {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();


        // TODO : Re-use the same user
        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();
        signUpBasicsScreen.skipSignUpDetails();
        suggestedUsersScreen = signUpBasicsScreen.waitForSuggestedUsers();
    }

}
