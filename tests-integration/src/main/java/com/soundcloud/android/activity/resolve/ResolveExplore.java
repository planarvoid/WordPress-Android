package com.soundcloud.android.activity.resolve;

import android.content.Intent;
import android.net.Uri;
import com.soundcloud.android.main.ResolveActivity;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;


//TODO: For some reason WebView kicks in before Resolver gets to play
public class ResolveExplore extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;

    public ResolveExplore() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/explore");
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(uri));
        super.setUp();
    }

    public void testResolveExploreUrl() throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
