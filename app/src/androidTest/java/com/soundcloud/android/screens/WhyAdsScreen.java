package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class WhyAdsScreen extends Screen {

    public WhyAdsScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement clickOK() {
        testDriver.findElement(text(testDriver.getString(android.R.string.ok))).click();
        waiter.waitForDialogToClose();
        return new VisualPlayerElement(testDriver);
    }

    @Override
    public boolean isVisible() {
        return testDriver.findElement(With.text(testDriver.getString(com.soundcloud.android.R.string.why_ads_dialog_message))).isVisible();
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

}
