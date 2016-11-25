package com.soundcloud.android.screens.go;

import com.soundcloud.android.R;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.UpgradeScreen;

public class GoOffboardingScreen extends Screen {
    public GoOffboardingScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return GoOffboardingActivity.class;
    }

    public UpgradeScreen clickResubscribe() {
        resubscribeButton().click();
        return new UpgradeScreen(testDriver);
    }

    public StreamScreen clickContinue() {
        continueButton().click();
        return new StreamScreen(testDriver);
    }

    public StreamScreen clickContinueRetry() {
        retryButton().click();
        return new StreamScreen(testDriver);
    }

    public UpgradeScreen clickResubscribeRetry() {
        retryButton().click();
        return new UpgradeScreen(testDriver);
    }

    private ViewElement resubscribeButton() {
        return testDriver
                .findOnScreenElement(With.text(R.string.go_offboarding_primary_button_text));
    }

    private ViewElement continueButton() {
        return testDriver
                .findOnScreenElement(With.text(R.string.go_offboarding_secondary_button_text));
    }

    public ViewElement retryButton() {
        return testDriver.findOnScreenElement(With.text(R.string.go_onboarding_retry));
    }
}
