package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.main.ResolveActivity;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;
import android.net.Uri;

public class ResolveTwitterUriTest extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected static PlayerScreen playerScreen;
    protected static ProfileScreen profileScreen;
    protected static Waiter waiter;

    public ResolveTwitterUriTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        Uri uri = TestConsts.TWITTER_SOUND_URI;
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(uri));
        super.setUp();

        waiter = new Waiter(solo);
        playerScreen = new PlayerScreen(solo);
    }

    public void testResolveExploreUrl() throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}