package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.upsellUser;

import com.soundcloud.android.framework.TestUser;

import android.net.Uri;

public class ResolveUpsellTrackingTest extends ResolveBaseTest {

    private static final String RESOLVE_UPSELL_REF = "resolve-upsell-ref";

    @Override
    protected Uri getUri() {
        return Uri.parse("soundcloud://soundcloudgo?ref=t6001");
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        startEventTracking();
    }

    public void testResolveUpsellTracksRefParam() {
        finishEventTracking(RESOLVE_UPSELL_REF);
    }

}
