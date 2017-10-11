package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.WhyAdsUpsellScreen;
import com.soundcloud.android.tests.player.ads.AdBaseTest;
import org.junit.Test;

import android.net.Uri;

@AdsTest
public class WhyAdsUpsellTest extends AdBaseTest {

    private static final String WHY_ADS_UPSELL_TEST_SCENARIO = "specs/why-ads-upsell-tracking-test.spec";

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    @org.junit.Ignore
    @Test
    public void testWhyAdsUpsellImpressionAndClick() throws Exception {
        swipeToAd();
        waiter.waitTwoSeconds();

        mrLocalLocal.startEventTracking();
        WhyAdsUpsellScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        UpgradeScreen upgradeScreen = dialog.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(WHY_ADS_UPSELL_TEST_SCENARIO);
    }

}
