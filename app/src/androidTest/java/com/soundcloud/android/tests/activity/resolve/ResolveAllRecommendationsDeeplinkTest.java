package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.properties.Flag.NEW_HOME;
import static com.soundcloud.android.tests.TestConsts.ALL_TRACK_RECOMMENDATIONS_DEEPLINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveAllRecommendationsDeeplinkTest extends ResolveBaseTest {

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().enable(NEW_HOME);
    }

    @Test
    public void testShouldOpenAllRecommendationsFromDeeplink() throws Exception {
        assertThat(new DiscoveryScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return ALL_TRACK_RECOMMENDATIONS_DEEPLINK;
    }
}
