package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.HomeScreen;

public class SignUpSpamDialogElement extends Element {

    public SignUpSpamDialogElement(Han solo) {
        super(solo, With.text(solo.getString(R.string.authentication_blocked_title)));
    }

    public HomeScreen clickCancelButton() {
        cancelButton().click();
        return new HomeScreen(solo);
    }

    private ViewElement cancelButton() {
        return solo.findElement(With.text(solo.getString(R.string.btn_cancel)));
    }
}
