package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.YouScreen;
import com.soundcloud.android.screens.elements.MainTabs;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.screens.record.RecordScreen;

public class MainNavigationHelper {

    private final Han testDriver;

    public MainNavigationHelper(Han solo) {
        this.testDriver = solo;
    }

    public StreamScreen goToStream() {
        return mainTabs().clickHome();
    }

    public ActivitiesScreen goToActivities() {
        return mainTabs().clickYou().clickActivitiesLink();
    }

    public CollectionsScreen goToCollections() {
        return mainTabs().clickCollections();
    }

    public TrackLikesScreen goToTrackLikes() {
        return mainTabs().clickCollections().clickTrackLikes();
    }

    public ProfileScreen goToMyProfile() {
        return mainTabs().clickYou().clickMyProfileLink();
    }

    public ExploreScreen goToExplore() {
        return mainTabs().clickYou().clickExploreLink();
    }

    public YouScreen goToYou() {
        return mainTabs().clickYou();
    }

    public RecordScreen goToRecord() {
        return mainTabs().clickYou().clickRecordScreen();
    }

    private MainTabs mainTabs() {
        return new MainTabs(testDriver);
    }

    public StationsScreen goToStationsHome() {
        throw new UnsupportedOperationException("There is no stations home in tabs");
    }
}
