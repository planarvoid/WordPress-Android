package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public abstract class FacebookResolveBaseTest extends ActivityTest<MainActivity> {

    public FacebookResolveBaseTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void setUp() throws Exception {
        final Intent activityIntent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class).setData(
                getUri());
        setActivityIntent(activityIntent);
        super.setUp();
    }

    protected abstract Uri getUri();
}
