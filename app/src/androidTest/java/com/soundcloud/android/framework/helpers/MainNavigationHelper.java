package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.BasicSettingsScreen;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.settings.LegalScreen;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.elements.MainTabs;
import com.soundcloud.android.screens.record.RecordScreen;

public class MainNavigationHelper {

    private final Han testDriver;

    public MainNavigationHelper(Han solo) {
        this.testDriver = solo;
    }

    public StreamScreen goToStream() {
        return mainTabs().clickHome();
    }

    public DiscoveryScreen goToDiscovery() {
        return mainTabs().clickDiscovery();
    }

    public DiscoveryScreen discoveryScreen() {
        return mainTabs().discovery();
    }

    public ActivitiesScreen goToActivities() {
        return mainTabs().clickMore().clickActivitiesLink();
    }

    public CollectionScreen goToCollections() {
        return mainTabs().clickCollections();
    }

    public TrackLikesScreen goToTrackLikes() {
        return mainTabs().clickCollections().clickLikedTracksPreview();
    }

    public ProfileScreen goToMyProfile() {
        return mainTabs().clickMore().clickMyProfileLink();
    }

    public MoreScreen goToMore() {
        return mainTabs().clickMore();
    }

    public LegalScreen goToLegal() {
        return goToMore().clickLegalLink();
    }

    public RecordScreen goToRecord() {
        return mainTabs().clickMore().clickRecordLink();
    }

    public BasicSettingsScreen goToBasicSettings() {
        return mainTabs().clickMore().clickBasicSettingsLink();
    }

    public OfflineSettingsScreen goToOfflineSettings() {
        return mainTabs().clickMore().clickOfflineSettingsLink();
    }

    private MainTabs mainTabs() {
        return new MainTabs(testDriver);
    }

}
