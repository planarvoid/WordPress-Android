package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.discovery.ViewAllTrackRecommendationsScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveAllRecommendationsDeeplinkTest extends ResolveBaseTest {

    public void testShouldOpenAllRecommendationsFromDeeplink() {
        assertThat(new ViewAllTrackRecommendationsScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.ALL_TRACK_RECOMMENDATIONS_DEEPLINK;
    }
}
