package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.explore.ExploreScreen;

public class MenuScreenTablet extends MenuScreen{

    public MenuScreenTablet(Han solo) {
        super(solo);
    }

    @Override
    public MyProfileScreen clickUserProfile() {
        userProfileMenuItem().click();
        return new MyProfileScreen(testDriver);
    }

    @Override
    public ExploreScreen clickExplore() {
        exploreMenuItem().click();
        return new ExploreScreen(testDriver);
    }

    @Override
    public TrackLikesScreen clickLikes() {
        likesMenuItem().click();
        return new TrackLikesScreen(testDriver);
    }

    @Override
    public PlaylistsScreen clickPlaylists() {
        playlistsMenuItem().click();
        return new PlaylistsScreen(testDriver);
    }

    public boolean isOpened() {
        return testDriver.findElement(With.id(R.id.fixed_navigation_fragment_id)).isVisible();
    }

}
