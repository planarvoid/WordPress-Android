package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.UpgradeScreen;

public class PlaylistUpsellCardElement {

    private final ViewElement wrapped;
    private final Han testDriver;

    public PlaylistUpsellCardElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public UpgradeScreen clickUpgrade() {
        upgradeButton().click();
        return new UpgradeScreen(testDriver);
    }

    private ViewElement upgradeButton() {
        return wrapped.findOnScreenElement(With.id(R.id.action_button));
    }
}
