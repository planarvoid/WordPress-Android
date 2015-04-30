package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailAgeOfMajority extends SignUpTest {

    public void testCanFollowAgeGatedProfile() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();
        signUpBasicsScreen.skipSignUpDetails();
        suggestedUsersScreen = signUpBasicsScreen.waitForSuggestedUsers();
        final EmailOptInScreen optInScreen = suggestedUsersScreen.finish();
        final StreamScreen streamScreen = optInScreen.clickNo();

        final PlaylistTagsScreen playlistTagsScreen = streamScreen.actionBar().clickSearchButton();
        final SearchResultsScreen searchResult = playlistTagsScreen.actionBar().doSearch("annoymouse");
        final ProfileScreen profileScreen = searchResult.clickFirstUserItem();
        assertThat(profileScreen.areCurrentlyFollowing(), is(false));

        profileScreen.clickFollowToggle();
        profileScreen.waitToBeFollowing();
        assertThat(profileScreen.areCurrentlyFollowing(), is(true));
    }
}
