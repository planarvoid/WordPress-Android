package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.VisualPrestitialActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ImageViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class VisualPrestitialScreen extends Screen {
    private static final Class ACTIVITY = VisualPrestitialActivity.class;

    public VisualPrestitialScreen(Han solo) {
        super(solo);
    }

    public boolean waitForImageToLoad() {
        return waiter.waitForElementImageToLoad(new ImageViewElement(imageView()));
    }

    public ViewElement imageView() {
        return testDriver.findOnScreenElement(With.id(R.id.ad_image_view));
    }

    public StreamScreen pressContinue() {
        continueButton().click();
        return new StreamScreen(testDriver);
    }

    public ViewElement continueButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_continue));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
