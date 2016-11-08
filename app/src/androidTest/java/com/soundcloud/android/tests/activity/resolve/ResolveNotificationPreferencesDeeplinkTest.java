package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.NotificationPreferencesScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveNotificationPreferencesDeeplinkTest extends ResolveBaseTest {

    public void testShouldOpenNotificationPreferences() {
        assertThat(new NotificationPreferencesScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.NOTIFICATION_PREFERENCES_URI_DEEPLINK;
    }

}
