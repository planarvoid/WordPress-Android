package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;

public class UserItemElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public UserItemElement(Han testDriver, ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public String getUsername() {
        return getText(wrapped.findElement(With.id(R.id.list_item_header)));
    }

    public ProfileScreen click() {
        wrapped.click();
        return new ProfileScreen(testDriver);
    }

    public UserItemElement toggleFollow() {
        getToggleFollowButton().click();
        return this;
    }

    public boolean isFollowing() {
        return getToggleFollowButton().isChecked();
    }

    private String getText(ViewElement element) {
        return new TextElement(element).getText();
    }

    private ViewElement getToggleFollowButton() {
        return wrapped.findElement(With.id(R.id.toggle_btn_follow));
    }
}
