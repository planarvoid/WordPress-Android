package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.StreamScreen;

public class FacebookInvitesItemElement {
    private final ViewElement wrapped;
    private final Han testDriver;
    private final Waiter waiter;

    public FacebookInvitesItemElement(Han testDriver, ViewElement wrapped) {
        this.wrapped = wrapped;
        this.testDriver = testDriver;
        this.waiter = new Waiter(testDriver);
    }

    public StreamScreen close() {
        wrapped.findElement(With.id(R.id.close_button)).click();
        waiter.waitForElementToBeInvisible(With.id(R.id.close_button));
        return new StreamScreen(testDriver);
    }

    public ViewElement getViewElement() {
        return wrapped;
    }
}
