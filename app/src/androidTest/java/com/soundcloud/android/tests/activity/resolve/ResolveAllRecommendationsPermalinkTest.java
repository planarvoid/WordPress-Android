package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveAllRecommendationsPermalinkTest extends ResolveBaseTest {

    @Override
    protected void beforeStartActivity() {
        getFeatureFlags().disable(Flag.NEW_HOME);
    }

    public void testShouldOpenAllRecommendationsFromPermalink() {
        assertThat(new StreamScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.ALL_TRACK_RECOMMENDATIONS_PERMALINK;
    }
}
