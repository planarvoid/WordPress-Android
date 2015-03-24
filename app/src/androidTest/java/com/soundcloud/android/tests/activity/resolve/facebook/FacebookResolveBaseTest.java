package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.Waiter;

import android.content.Intent;
import android.net.Uri;

public abstract class FacebookResolveBaseTest extends ActivityTest<MainActivity> {

    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected Waiter waiter;

    public FacebookResolveBaseTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        final Intent activityIntent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class).setData(getUri());
        setActivityIntent(activityIntent);
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Uri getUri();
}
