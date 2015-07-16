package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;


//TODO: For some reason WebView kicks in before Resolver gets to play
public class ResolveExploreTest extends ActivityTest<ResolveActivity> {
    public ResolveExploreTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        Uri uri = Uri.parse("http://soundcloud.com/explore");
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
