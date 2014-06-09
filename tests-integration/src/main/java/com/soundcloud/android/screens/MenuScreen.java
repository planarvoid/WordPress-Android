package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.elements.ActionBarElement;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.tests.with.With;

import android.os.Build;

public class MenuScreen {
    private final ActionBarElement actionBar;
    protected Han solo;
    protected Waiter waiter;
    protected int explore_selector = R.string.side_menu_explore;
    protected int likes_selector = R.string.side_menu_likes;
    protected int playlist_selector = R.string.side_menu_playlists;
    protected final int username_selector = R.id.username;

    public MenuScreen(Han solo) {
        this.solo = solo;
        this.actionBar = new ActionBarElement(solo);
        this.waiter = new Waiter(solo);
    }

    public HomeScreen logout() {
        solo.openSystemMenu();
        solo.clickOnActionBarItem(R.id.action_settings);
        new SettingsScreen(solo);
        solo.findElement(With.text(solo.getString(R.string.pref_revoke_access))).click();
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnText(android.R.string.ok);
        return new HomeScreen(solo);
    }

    //TODO: Move this to systemSettingsScreen
    public SettingsScreen clickSystemSettings() {
        solo.clickOnActionBarItem(R.id.action_settings);
        return new SettingsScreen(solo);
    }

    private ListElement menuContainer() {
        return solo.findElement(With.id(R.id.nav_listview)).toListView();
    }

    protected ViewElement userProfileMenuItem() {
        return menuContainer().getItemAt(0);
    }

    protected ViewElement streamMenuItem() {
        return menuContainer().getItemAt(1);
    }

    protected ViewElement exploreMenuItem() {
        return menuContainer().getItemAt(2);
    }

    protected ViewElement likesMenuItem() {
        return menuContainer().getItemAt(3);
    }

    protected ViewElement playlistsMenuItem() {
        return menuContainer().getItemAt(4);
    }

    protected ViewElement usernameLabel() {
        return userProfileMenuItem().findElement(With.id(username_selector));
    }

    //TODO: move this to ActionBarScreen
    public MenuScreen open() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            actionBar.clickHomeButton();
        } else {
            solo.findElement(With.id(R.id.up)).click();
        }

        waiter.waitForDrawerToOpen();
        return new MenuScreen(solo);
    }

    public String getUserName() {
        return usernameLabel().getText();
    }

    public boolean isOpened() {
        return solo.findElement(With.id(R.id.navigation_fragment_id)).isVisible();
    }

    public ProfileScreen clickUserProfile() {
        userProfileMenuItem().click();
        waiter.waitForDrawerToClose();
        return new MyProfileScreen(solo);
    }

    public ExploreScreen clickExplore() {
        exploreMenuItem().click();
        waiter.waitForDrawerToClose();
        return new ExploreScreen(solo);
    }

    public LikesScreen clickLikes() {
        likesMenuItem().click();
        waiter.waitForDrawerToClose();
        return new LikesScreen(solo);
    }

    public PlaylistScreen clickPlaylist() {
        playlistsMenuItem().click();
        waiter.waitForDrawerToClose();
        return new PlaylistScreen(solo);
    }
}
