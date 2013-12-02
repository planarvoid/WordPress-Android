package com.soundcloud.android.activity.resolve.facebook;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;
import android.net.Uri;

public abstract class FacebookResolveBaseTest extends ActivityTestCase<MainActivity> {

    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected Waiter waiter;

    public FacebookResolveBaseTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        final Intent activityIntent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class).setData(getUri());
        setActivityIntent(activityIntent);
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Uri getUri();
}
