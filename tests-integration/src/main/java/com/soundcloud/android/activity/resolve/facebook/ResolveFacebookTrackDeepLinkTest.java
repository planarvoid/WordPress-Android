package com.soundcloud.android.activity.resolve.facebook;

import android.content.Intent;
import android.net.Uri;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class ResolveFacebookTrackDeepLinkTest extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected static LegacyPlayerScreen playerScreen;
    protected static ProfileScreen profileScreen;
    protected static Waiter waiter;

    public ResolveFacebookTrackDeepLinkTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        setActivityIntent(createIntent(TestConsts.FACEBOOK_SOUND_DEEP_LINK));

        super.setUp();

        waiter = new Waiter(solo);
        playerScreen = new LegacyPlayerScreen(solo);
    }

    private Intent createIntent(Uri uri) {
        return new Intent(Intent.ACTION_VIEW).setData(uri);
    }

    public void testStartPlayerActivityWhenTrackUrnIsValid() throws Exception {
        solo.assertActivity(com.soundcloud.android.playback.PlayerActivity.class, DEFAULT_WAIT);
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }

}
