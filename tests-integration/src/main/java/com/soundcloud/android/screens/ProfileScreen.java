package com.soundcloud.android.screens;

import android.view.View;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.utils.Log;

public class ProfileScreen extends Screen {
    private static Class ACTIVITY = ProfileActivity.class;

    public ProfileScreen(Han solo) {
        super(solo);
        waitForActivity();
    }

    private View followButton() {
        return solo.getView(R.id.toggle_btn_follow);
    }
    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String userName() {
        return getTextById(R.id.username);
    }

    public String location() {
        return getTextById(R.id.location);
    }

    public String followingMessage() {
        return getTextById(R.id.followers_message);
    }

    public void clickFollowToggle() {
        solo.clickOnView(R.id.toggle_btn_follow);
    }

    public boolean isFollowButtonVisible() {
        return followButton().getVisibility() == View.VISIBLE;
    }

    private String getTextById(int id) {
        TextView location = (TextView) solo.getView(id);
        return location.getText().toString();
    }

    protected void waitForActivity() {
        //Todo: wait for element
        waiter.waitForElement(followButton());
        waiter.waitForActivity(ACTIVITY);
    }

}