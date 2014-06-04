package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.Han;

public class MenuScreenTablet extends MenuScreen{

    public MenuScreenTablet(Han solo) {
        super(solo);
    }

    @Override
    public MyProfileScreen clickProfile() {
        solo.clickOnView(profiles_selector);
        return new MyProfileScreen(solo);
    }

    @Override
    public ExploreScreen clickExplore() {
        solo.clickOnText(explore_selector);
        return new ExploreScreen(solo);
    }

    @Override
    public LikesScreen clickLikes() {
        solo.clickOnText(likes_selector);
        return new LikesScreen(solo);
    }

    @Override
    public PlaylistScreen clickPlaylist() {
        solo.clickOnText(playlist_selector);
        return new PlaylistScreen(solo);
    }

    public boolean isOpened() {
        return solo.findElement(R.id.fixed_navigation_fragment_id).isVisible();
    }

}
