package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;

public class WhyAdsUpsellScreen extends WhyAdsScreen {

    public WhyAdsUpsellScreen(Han solo) {
        super(solo);
    }

    public UpgradeScreen clickUpgrade() {
        testDriver.findOnScreenElement(text(testDriver.getString(R.string.upsell_remove_ads))).click();
        return new UpgradeScreen(testDriver);
    }

    @Override
    protected int getDialogMessageId() {
        return R.string.ads_why_ads_upsell_dialog_message;
    }
}
