package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.auth.SignUpTest;
import org.junit.Ignore;

public class ByEmailAgeOfMajority extends SignUpTest {

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }

    @Ignore // https://soundcloud.atlassian.net/browse/DROID-1697
    public void testCanFollowAgeGatedProfile() throws Exception {
        addMockedResponse(ApiEndpoints.SIGN_UP.path(), "sign-up-success.json");

        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(21)
                .signup()
                .acceptTerms()
                .saveSignUpDetails();

        final ProfileScreen profileScreen = mainNavHelper.goToOldDiscovery()
                                                         .clickSearch()
                                                         .doSearch("annoymouse")
                                                         .goToPeopleTab()
                                                         .findAndClickFirstUserItem();

        boolean alreadyFollowing = profileScreen.areCurrentlyFollowing();

        if (alreadyFollowing) {
            profileScreen.clickFollowToggle();
            profileScreen.waitToNotBeFollowing();
        }

        profileScreen.clickFollowToggle();
        profileScreen.waitToBeFollowing();
        assertThat(profileScreen.areCurrentlyFollowing(), is(true));
    }
}
