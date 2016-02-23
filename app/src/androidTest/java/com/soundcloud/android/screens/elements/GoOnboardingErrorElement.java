package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.CollectionScreen;

public class GoOnboardingErrorElement extends Element {

    public GoOnboardingErrorElement(Han solo) {
        // This is the id currently used by Android to
        // display content in an alert.
        //
        // Since we don't have any custom view, it is the
        // only way I could find to elect the view.
        super(solo, With.id(android.R.id.content));
    }

    public CollectionScreen clickTryLater() {
        testDriver.findOnScreenElement(With.text(R.string.go_onboarding_error_dialog_button)).click();
        return new CollectionScreen(testDriver);
    }
}
