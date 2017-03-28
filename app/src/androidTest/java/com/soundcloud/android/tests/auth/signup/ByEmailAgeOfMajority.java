package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailAgeOfMajority extends SignUpTest {

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }

    public void testCanFollowAgeGatedProfile() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(21)
                .signup()
                .acceptTerms()
                .saveSignUpDetails();

        final ProfileScreen profileScreen = mainNavHelper.goToDiscovery()
                                                         .clickSearch()
                                                         .doSearch("annoymouse")
                                                         .goToPeopleTab()
                                                         .findAndClickFirstUserItem();

        assertThat(profileScreen.areCurrentlyFollowing(), is(false));

        profileScreen.clickFollowToggle();
        profileScreen.waitToBeFollowing();
        assertThat(profileScreen.areCurrentlyFollowing(), is(true));
    }
}
