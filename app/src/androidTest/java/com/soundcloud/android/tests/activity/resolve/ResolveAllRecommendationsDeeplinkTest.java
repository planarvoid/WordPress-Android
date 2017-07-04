package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveAllRecommendationsDeeplinkTest extends ResolveBaseTest {

    @Override
    protected void beforeStartActivity() {
        getFeatureFlags().enable(Flag.NEW_HOME);
    }

    public void testShouldOpenAllRecommendationsFromDeeplink() {
        assertThat(new DiscoveryScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.ALL_TRACK_RECOMMENDATIONS_DEEPLINK;
    }
}
