package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.StreamScreen;

public class FacebookInvitesItemElement {
    private final ViewElement wrapped;
    private final Han testDriver;

    public FacebookInvitesItemElement(Han testDriver, ViewElement wrapped) {
        this.wrapped = wrapped;
        this.testDriver = testDriver;
    }

    public StreamScreen close() {
        wrapped.findElement(With.id(R.id.close_button)).click();
        return new StreamScreen(testDriver);
    }

    public ViewElement getViewElement() {
        return wrapped;
    }
}
