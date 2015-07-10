package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public abstract class FacebookResolveBaseTest extends ActivityTest<MainActivity> {

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
        waiter.waitFiveSeconds();
    }

    protected abstract Uri getUri();
}
