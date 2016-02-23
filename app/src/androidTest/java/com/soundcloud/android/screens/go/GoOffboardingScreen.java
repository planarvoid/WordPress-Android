package com.soundcloud.android.screens.go;

import com.soundcloud.android.R;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
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
        testDriver
                .findOnScreenElement(With.text(R.string.go_offboarding_primary_button_text))
                .click();

        return new UpgradeScreen(testDriver);
    }

}
