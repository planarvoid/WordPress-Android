package com.soundcloud.android.tests.profile;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.FollowingsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class ProfileErrorTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen profileScreen;
    private StubMapping stubMapping;
    private FollowingsScreen followingsScreen;

    public ProfileErrorTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return profileEntryUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        followingsScreen = mainNavHelper.goToMyProfile()
                                        .touchInfoTab()
                                        .clickFollowingsLink();

        UserItemElement userItemElement = followingsScreen.getUsers().get(0);

        stubMapping = stubFor(get(urlPathMatching("/users(.*)/profile/v2"))
                                      .willReturn(aResponse().withStatus(500)));

        profileScreen = userItemElement.click();
    }

    @Test
    public void testConnectionErrorAndRetryInPosts() throws Exception {
        assertTrue(profileScreen.scrollToErrorView().isOnScreen());

        removeStub(stubMapping);

        profileScreen.pullToRefresh();

        assertThat(profileScreen.currentItemCount(), is(greaterThan(0)));
    }
}
