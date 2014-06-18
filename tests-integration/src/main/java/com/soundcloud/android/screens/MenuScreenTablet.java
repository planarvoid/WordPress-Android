package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.with.With;

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
    public LikesScreen clickLikes() {
        likesMenuItem().click();
        return new LikesScreen(testDriver);
    }

    @Override
    public PlaylistScreen clickPlaylist() {
        playlistsMenuItem().click();
        return new PlaylistScreen(testDriver);
    }

    public boolean isOpened() {
        return testDriver.findElement(With.id(R.id.fixed_navigation_fragment_id)).isVisible();
    }

}
