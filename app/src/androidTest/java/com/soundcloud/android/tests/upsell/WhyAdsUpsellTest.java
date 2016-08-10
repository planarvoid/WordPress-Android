package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.WhyAdsUpsellScreen;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.player.ads.AdBaseTest;

import android.net.Uri;

@AdsTest
public class WhyAdsUpsellTest extends AdBaseTest {

    private static final String WHY_ADS_UPSELL_TEST_SCENARIO = "why-ads-upsell-tracking-test";

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    public void testWhyAdsUpsellImpressionAndClick() {
        swipeToAd();
        waiter.waitTwoSeconds();

        startEventTracking();
        WhyAdsUpsellScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        UpgradeScreen upgradeScreen = dialog.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(WHY_ADS_UPSELL_TEST_SCENARIO);
    }

}