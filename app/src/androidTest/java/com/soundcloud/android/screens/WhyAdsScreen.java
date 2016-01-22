package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class WhyAdsScreen extends Screen {

    public WhyAdsScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickOK() {
        testDriver.findOnScreenElement(text(testDriver.getString(android.R.string.ok))).click();
        waiter.waitForDialogToClose();
        return new VisualPlayerElement(testDriver);
    }

    public UpgradeScreen clickUpgrade() {
        testDriver.findOnScreenElement(text(testDriver.getString(R.string.upsell_remove_ads))).click();
        return new UpgradeScreen(testDriver);
    }

    @Override
    public boolean isVisible() {
        return testDriver.findOnScreenElement(With.text(testDriver.getString(com.soundcloud.android.R.string.ads_why_ads_dialog_message))).isOnScreen();
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

}
