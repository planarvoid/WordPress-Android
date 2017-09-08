package com.soundcloud.android.tests.ageGating;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.over21user;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class FollowingAgeGatedUser extends ResolveBaseTest {

    public static final String emptyFollowings = "{\"collection\":[],\"_links\":{}}";
    public static final String oneFollowings = "{\"collection\":[{\"created\":\"2017/08/21 07:32:19 +0000\",\"target\":\"soundcloud:users:32326572\",\"user\":\"soundcloud:users:149060192\"}],\"_links\":{}}";

    @Override
    protected Uri getUri() {
        return TestConsts.USER_ANNOYMOUSE;
    }

    @Override
    protected TestUser getUserForLogin() {
        return over21user;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // This is probably too late :/
        addMockedStringResponse(ApiEndpoints.MY_FOLLOWINGS.path(), 200, emptyFollowings);
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathMatching(".*follows/users/.*"))
                        .willReturn(aResponse().withStatus(201)));
    }

    @Test
    public void testAbove21UsersAreAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = new ProfileScreen(solo);
        addMockedStringResponse(ApiEndpoints.MY_FOLLOWINGS.path(), 200, oneFollowings);
        annoyMouseUserScreen.clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(true));
    }
}
