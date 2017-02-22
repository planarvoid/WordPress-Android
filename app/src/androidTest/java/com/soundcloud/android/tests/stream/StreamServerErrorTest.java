package com.soundcloud.android.tests.stream;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class StreamServerErrorTest extends ActivityTest<MainActivity> {

    protected StreamScreen streamScreen;

    public StreamServerErrorTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(get(urlPathEqualTo(ApiEndpoints.STREAM.path()))
                        .willReturn(aResponse().withStatus(500)));
    }

    public void testShowsStreamServerError() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        assertTrue(streamScreen.emptyView().isVisible());
        assertThat(streamScreen.emptyView().message(), is(equalTo(getSolo().getString(R.string.ak_error_soundcloud_no_response))));
    }
}
