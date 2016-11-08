package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveOfflineSettingsPermalinkTest extends ResolveBaseTest {

    public void testShouldOpenOfflineSettings() {
        assertThat(new OfflineSettingsScreen(solo), is(visible()));
    }

    @Override
    protected void logInHelper() {
        upsellUser.logIn(getInstrumentation().getTargetContext());
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.OFFLINE_SETTINGS_URI_PERMALINK;
    }

}
