package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.ForceUpdateDialog;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.Element;

public class ForceUpdateDialogElement extends Element {

    public ForceUpdateDialogElement(Han solo) {
        super(solo, With.text(solo.getString(R.string.kill_switch_message)));
        waiter.assertForFragmentByTag(ForceUpdateDialog.DIALOG_TAG);
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(ForceUpdateDialog.DIALOG_TAG);
    }

    public ForceUpdateDialogElement clickUpgrade() {
        getUpgradeButton().click();
        return this;
    }

    private ViewElement getUpgradeButton() {
        return testDriver.findOnScreenElement(With.text(R.string.kill_switch_confirm));
    }
}
