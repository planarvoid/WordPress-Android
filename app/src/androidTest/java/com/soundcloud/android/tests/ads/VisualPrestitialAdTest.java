package com.soundcloud.android.tests.ads;

import static com.soundcloud.android.framework.TestUser.adPrestitialUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.VisualPrestitialScreen;
import com.soundcloud.android.tests.ActivityTest;

@AdsTest
public class VisualPrestitialAdTest extends ActivityTest<LauncherActivity> {

    private VisualPrestitialScreen prestitialScreen;

    public VisualPrestitialAdTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return adPrestitialUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        prestitialScreen = new VisualPrestitialScreen(solo);
    }

    public void testAdLoadsAndIsDismissed() throws Exception {
        mrLocalLocal.startEventTracking();

        assertThat(prestitialScreen.waitForImageToLoad(), is(true));
        final StreamScreen stream = prestitialScreen.pressContinue();

        assertThat(stream, is(visible()));

        mrLocalLocal.verify("specs/visual_prestitial.spec");
    }
}
