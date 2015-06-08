package com.soundcloud.android.screens;

import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

import java.util.List;

public class ActivitiesScreen extends Screen {

    public ActivitiesScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ActivitiesActivity.class;
    }

    public ProfileScreen clickFollower() {
        return clickToProfile("started following you");
    }

    public ProfileScreen clickLike() {
        return clickToProfile("liked ");
    }

    public ProfileScreen clickRepost() {
        return clickToProfile("reposted ");
    }

    public TrackCommentsScreen clickComment() {
        cellElementsWithText("commented on ").get(0).click();
        return new TrackCommentsScreen(testDriver);
    }

    private List<ViewElement> cellElementsWithText(String text) {
        waiter.waitForElement(With.textContaining(text));
        return testDriver.findElements(With.textContaining(text));
    }

    private ProfileScreen clickToProfile(String textToClick) {
        cellElementsWithText(textToClick).get(0).click();
        return new ProfileScreen(testDriver);
    }
}
