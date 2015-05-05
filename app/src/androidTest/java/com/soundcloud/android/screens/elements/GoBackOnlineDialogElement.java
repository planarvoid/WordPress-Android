package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

public class GoBackOnlineDialogElement extends Element {

    public GoBackOnlineDialogElement(Han solo) {
        super(solo, With.id(R.id.go_back_online_dialog));
    }

    public void clickContinue() {
        solo.findElement(text(solo.getString(R.string.offline_dialog_go_online_continue))).click();
    }
}
