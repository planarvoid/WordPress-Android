package com.soundcloud.android.tests.activity.resolve;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.OFFLINE_SETTINGS_URI_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveOfflineSettingsPermalinkTest extends ResolveBaseTest {

    @Test
    public void testShouldOpenOfflineSettings() throws Exception {
        assertThat(new OfflineSettingsScreen(solo), is(visible()));
    }

    @Override
    protected TestUser getUserForLogin() {
        return offlineUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableOfflineContent(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return OFFLINE_SETTINGS_URI_PERMALINK;
    }

}
