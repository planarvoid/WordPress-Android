package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.NOTIFICATION_PREFERENCES_URI_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.NotificationPreferencesScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveNotificationPreferencesPermalinkTest extends ResolveBaseTest {

    @Test
    public void testShouldOpenNotificationPreferences() throws Exception {
        assertThat(new NotificationPreferencesScreen(solo), is(visible()));
    }

    @Override
    protected Uri getUri() {
        return NOTIFICATION_PREFERENCES_URI_PERMALINK;
    }

}
