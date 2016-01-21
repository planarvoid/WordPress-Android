package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class GoBackOnlineDialogElement extends Element {

    private With continueButtonLocator = text(testDriver.getString(R.string.offline_dialog_go_online_continue));

    public GoBackOnlineDialogElement(Han solo) {
        super(solo, With.id(R.id.go_back_online_dialog));
    }

    public void clickContinue() {
        continueButton().click();
        waiter.waitForElementToBeInvisible(continueButtonLocator);
    }

    private ViewElement continueButton() {
        return testDriver.findOnScreenElement(continueButtonLocator);
    }
}
