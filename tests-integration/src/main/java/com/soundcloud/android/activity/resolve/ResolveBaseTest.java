package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.activity.ResolveActivity;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;
import android.net.Uri;

public abstract class ResolveBaseTest extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected static PlayerScreen playerScreen;
    protected static ProfileScreen profileScreen;
    protected static Waiter waiter;

    public ResolveBaseTest() {
        super(ResolveActivity.class);
    }

    protected abstract Uri getUri();

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        super.setUp();
        waiter = new Waiter(solo);

        profileScreen = new ProfileScreen(solo);
        playerScreen = new PlayerScreen(solo);
    }

}
