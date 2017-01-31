package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.BasicSettingsScreen;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.OfflineSettingsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.MoreScreen;
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

    public DiscoveryScreen goToDiscovery() {
        // TODO: Fix horrible hack after Support Library bump.
        // Bug: When Robotium clicks on the Search Icon in the main nav, the app switches to the Discovery screen
        // then performs a sudden scrollDown. This leads all tests that search for the `search_text` view to fail.
        //
        // As a quick fix, simply double-tap the discovery tab. This will force the search screen to scroll to the top.
        final MainTabs mainTabs = mainTabs();

        mainTabs.clickDiscovery();

        return mainTabs.clickDiscovery();
    }

    public MoreScreen goToMore() {
        return mainTabs().clickMore();
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
