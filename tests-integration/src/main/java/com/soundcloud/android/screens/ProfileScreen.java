package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

public class ProfileScreen extends Screen {
    private static Class ACTIVITY = ProfileActivity.class;

    public ProfileScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement playTrack(int index) {
        waiter.waitForElements(R.id.track_list_item);
        tracks().get(index).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    private java.util.List<ViewElement> tracks() {
        return testDriver.findElements(With.id(R.id.track_list_item));
    }

    private ViewElement followButton() {
        return testDriver.findElement(With.id(R.id.toggle_btn_follow));
    }

    private ViewElement userName() {
        return testDriver.findElement(With.id(R.id.username));
    }

    private ViewElement location() {
        return testDriver.findElement(With.id(R.id.location));
    }

    private ViewElement followersMessage(){
        return testDriver.findElement(With.id(R.id.followers_message));
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
