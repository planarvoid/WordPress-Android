package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.ViewElement;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.TextView;

public class ProfileScreen extends Screen {
    private static Class ACTIVITY = ProfileActivity.class;

    public ProfileScreen(Han solo) {
        super(solo);
    }

    private ViewElement followButton() {
        return solo.findElement(R.id.toggle_btn_follow);
    }

    private ViewElement userName() {
        return solo.findElement(R.id.username);
    }

    private ViewElement location() {
        return solo.findElement(R.id.location);
    }

    private ViewElement followersMessage(){
        return solo.findElement(R.id.followers_message);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getUserName() {
        return userName().getText();
    }

    public String getLocation() {
        return location().getText();
    }

    public String getFollowersMessage() {
        return followersMessage().getText();
    }

    public void clickFollowToggle() {
        followButton().click();
    }

    public boolean isFollowButtonVisible() {
        return followButton().isVisible();
    }
}
