package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.BrokenSearchTest;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.LegacySearchResultsScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailAgeOfMajority extends SignUpTest {

    @BrokenSearchTest
    public void testCanFollowAgeGatedProfile() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        EmailOptInScreen optInScreen = signUpBasicsScreen
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(21)
                .signup()
                .acceptTerms()
                .skipSignUpDetails();

        final StreamScreen streamScreen = optInScreen.clickNo();
        final PlaylistTagsScreen playlistTagsScreen = streamScreen.actionBar().clickSearchButton();
        final LegacySearchResultsScreen searchResult = playlistTagsScreen.actionBar().doLegacySearch("annoymouse");
        final ProfileScreen profileScreen = searchResult.clickFirstUserItem();
        assertThat(profileScreen.areCurrentlyFollowing(), is(false));

        profileScreen.clickFollowToggle();
        profileScreen.waitToBeFollowing();
        assertThat(profileScreen.areCurrentlyFollowing(), is(true));
    }
}
