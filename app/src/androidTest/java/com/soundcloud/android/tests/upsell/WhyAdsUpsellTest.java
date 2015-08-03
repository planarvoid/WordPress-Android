package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.AdsTest;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.player.ads.AdBaseTest;

import android.net.Uri;

@AdsTest
public class WhyAdsUpsellTest extends AdBaseTest {

    private static final String WHY_ADS_UPSELL_TEST_SCENARIO = "why-ads-upsell-tracking-test";

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.OFFLINE_SYNC);
        super.setUp();
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    public void ignore_testWhyAdsUpsellImpressionAndClick() {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAds();
        assertThat(dialog, is(visible()));

        UpgradeScreen upgradeScreen = dialog.clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        verifier.assertScenario(WHY_ADS_UPSELL_TEST_SCENARIO);
    }

}
