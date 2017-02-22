package com.soundcloud.android.tests.profile;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.ActivityTest;

public class OtherProfileErrorTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen profileScreen;
    private StubMapping stubMapping;

    public OtherProfileErrorTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return profileEntryUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile()
                                     .touchFollowingsTab();

        UserItemElement userItemElement = profileScreen.getUsers().get(0);

        stubMapping = stubFor(get(urlPathMatching("/users(.*)/profile/v2"))
                        .willReturn(aResponse().withStatus(500)));

        userItemElement.click();
    }

    public void testConnectionErrorAndRetryInPosts() {
        assertTrue(profileScreen.errorView().isOnScreen());

        removeStub(stubMapping);

        profileScreen.pullToRefresh();

        assertThat(profileScreen.currentItemCount(), is(greaterThan(0)));
    }
}
