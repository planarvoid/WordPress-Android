package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.ActivitiesScreen;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.explore.ExploreScreen;

public class MainNavigationHelper {

    private final MenuScreen menuScreen;
    private final ToolBarElement toolbar;

    public MainNavigationHelper(Han solo) {
        this.menuScreen = new MenuScreen(solo);
        this.toolbar = new ToolBarElement(solo);
    }

    public StreamScreen goToStream() {
        return menuScreen.open().clickStream();
    }

    public ActivitiesScreen goToActivities() {
        return toolbar.clickActivityOverflowButton();
    }

    public CollectionsScreen goToCollections() {
        return menuScreen.open().clickCollections();
    }

    public TrackLikesScreen goToTrackLikes() {
        return menuScreen.open().clickLikes();
    }

    public ProfileScreen goToMyProfile() {
        return menuScreen.open().clickUserProfile();
    }

    public ExploreScreen goToExplore() {
        return menuScreen.open().clickExplore();
    }

    public StationsScreen goToStationsHome() {
        return menuScreen.open().clickStations();
    }

    @Deprecated // use Collections
    public PlaylistsScreen goToPlaylists() {
        return menuScreen.open().clickPlaylists();
    }
}
