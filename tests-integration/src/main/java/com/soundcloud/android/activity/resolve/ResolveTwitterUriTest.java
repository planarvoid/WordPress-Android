package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;
import android.net.Uri;

public class ResolveTwitterUriTest extends ActivityTestCase<ResolveActivity> {
    protected static VisualPlayerElement playerScreen;
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
        playerScreen = new VisualPlayerElement(solo);
    }

    public void testResolveExploreUrl() {
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
