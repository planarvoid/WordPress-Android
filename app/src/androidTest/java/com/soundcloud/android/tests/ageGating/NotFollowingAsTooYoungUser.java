package com.soundcloud.android.tests.ageGating;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.childUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class NotFollowingAsTooYoungUser extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.USER_ANNOYMOUSE;
    }

    @Override
    protected TestUser getUserForLogin() {
        return childUser; // As the follow request is mocked, the user doesn't matter
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathMatching(".*follows/users/.*"))
                        .willReturn(aResponse().withStatus(400).withBody("{\"error_key\": \"age_unknown\"}")));
    }

    public void testBelow18UsersAreNotAbleToFollowAgeGatedUsers() {
        ProfileScreen annoyMouseUserScreen = new ProfileScreen(solo);
        annoyMouseUserScreen.clickFollowToggle();

        assertThat(annoyMouseUserScreen.areCurrentlyFollowing(), is(false));
    }
}

