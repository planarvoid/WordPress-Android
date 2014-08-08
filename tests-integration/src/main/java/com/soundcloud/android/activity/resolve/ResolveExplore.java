package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

import android.content.Intent;
import android.net.Uri;


//TODO: For some reason WebView kicks in before Resolver gets to play
public class ResolveExplore extends ActivityTestCase<ResolveActivity> {
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

    public void testResolveExploreUrl() {
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
