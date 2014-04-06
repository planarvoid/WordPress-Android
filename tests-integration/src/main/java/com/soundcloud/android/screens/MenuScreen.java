package com.soundcloud.android.screens;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.R.id;
import com.soundcloud.android.R.string;
import com.soundcloud.android.main.NavigationDrawerFragment;
import com.soundcloud.android.main.NavigationFragment;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class MenuScreen {
    protected Han solo;
    protected Waiter waiter;
    protected int explore_selector = R.string.side_menu_explore;
    protected int likes_selector = R.string.side_menu_likes;
    protected int playlist_selector = string.side_menu_playlists;
    protected int profiles_selector = id.username;

    public MenuScreen(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }

    public HomeScreen logout() {
        solo.openSystemMenu();
        solo.clickOnActionBarItem(R.id.action_settings);
        new SettingsScreen(solo);
        solo.clickOnText(R.string.pref_revoke_access);
        solo.assertText(R.string.menu_clear_user_title);
        solo.clickOnOK();
        return new HomeScreen(solo);
    }

    //TODO: Move this to systemSettingsScreen
    public SettingsScreen clickSystemSettings() {
        solo.clickOnActionBarItem(id.action_settings);
        return new SettingsScreen(solo);
    }

    public ListView rootMenu() {
        return (ListView) solo.waitForViewId(R.id.nav_listview, 20000);
    }

    public View youMenu() {
        return rootMenu().getChildAt(NavigationFragment.NavItem.PROFILE.ordinal());
    }

    //TODO: move this to ActionBarScreen
    public MenuScreen open() {
        solo.getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                solo.clickOnActionBarHomeButton();
            }
        });
        waiter.waitForDrawerToOpen();
        return new MenuScreen(solo);
    }

    public String getUserName() {
        TextView you = (TextView) youMenu().findViewById(R.id.username);
        return you.getText().toString();
    }

    public void openExplore() {
        solo.clickOnActionBarHomeButton();
        waiter.waitForDrawerToOpen();
        solo.clickOnText(explore_selector);
        waiter.waitForViewId(android.R.id.list);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public boolean isOpened() {
        NavigationDrawerFragment navigationDrawerFragment = null;
        try {
            navigationDrawerFragment = solo.getCurrentNavigationDrawer();
        } catch (Exception e) {
            return false;
        }
            return navigationDrawerFragment.isDrawerOpen();
    }

    public ProfileScreen clickProfile() {
        solo.clickOnView(R.id.username);
        waiter.waitForDrawerToClose();
        return new MyProfileScreen(solo);
    }

    public ExploreScreen clickExplore() {
        solo.clickOnText(explore_selector);
        waiter.waitForDrawerToClose();
        return new ExploreScreen(solo);
    }

    public LikesScreen clickLikes() {
        solo.clickOnText(likes_selector);
        waiter.waitForDrawerToClose();
        return new LikesScreen(solo);
    }

    public PlaylistScreen clickPlaylist() {
        solo.clickOnText(playlist_selector);
        waiter.waitForDrawerToClose();
        return new PlaylistScreen(solo);
    }
}
