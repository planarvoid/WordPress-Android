package com.soundcloud.android.tests.activity.resolve;

import static android.net.Uri.parse;
import static com.soundcloud.android.framework.TestUser.upsellUser;

import com.soundcloud.android.framework.TestUser;
import org.junit.Test;

import android.net.Uri;

public class ResolveUpsellTrackingTest extends ResolveBaseTest {

    private static final String RESOLVE_UPSELL_REF = "specs/resolve-upsell-ref-v1.spec";

    @Override
    protected Uri getUri() {
        return parse("soundcloud://soundcloudgo?ref=t6001");
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        mrLocalLocal.startEventTracking();
    }

    @Test
    public void testResolveUpsellTracksRefParam() throws Exception {
        mrLocalLocal.verify(RESOLVE_UPSELL_REF);
    }

}
