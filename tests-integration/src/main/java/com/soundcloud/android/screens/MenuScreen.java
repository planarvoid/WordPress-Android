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

import java.util.List;

public class MenuScreen {
    private final ActionBarElement actionBar;
    protected Han testDriver;
    protected Waiter waiter;
    protected final int username_selector = R.id.username;

    public MenuScreen(Han solo) {
        this.testDriver = solo;
        this.actionBar = new ActionBarElement(solo);
        this.waiter = new Waiter(solo);
    }

    public HomeScreen logout() {
        testDriver.openSystemMenu();
        testDriver.clickOnActionBarItem(R.id.action_settings);
        new SettingsScreen(testDriver);
        testDriver.findElement(With.text(testDriver.getString(R.string.pref_revoke_access))).click();
        testDriver.assertText(R.string.menu_clear_user_title);
        testDriver.clickOnText(android.R.string.ok);
        return new HomeScreen(testDriver);
    }

    //TODO: Move this to systemSettingsScreen
    public SettingsScreen clickSystemSettings() {
        testDriver.clickOnActionBarItem(R.id.action_settings);
        return new SettingsScreen(testDriver);
    }

    private ListElement menuContainer() {
        return testDriver.findElement(With.id(R.id.nav_listview)).toListView();
    }

    protected ViewElement userProfileMenuItem() {
        return menuContainer().getItemAt(0);
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
            testDriver.findElement(With.id(R.id.up)).click();
        }

        waiter.waitForDrawerToOpen();
        return new MenuScreen(testDriver);
    }

    public String getUserName() {
        return usernameLabel().getText();
    }

    public boolean isOpened() {
        List<ViewElement> menuDrawers = testDriver.findElements(With.id(R.id.navigation_fragment_id));
        ViewElement menuDrawer = menuDrawers.isEmpty() ? null : menuDrawers.get(0);
        return menuDrawer != null && menuDrawer.isVisible();
    }

    public ProfileScreen clickUserProfile() {
        userProfileMenuItem().click();
        waiter.waitForDrawerToClose();
        return new MyProfileScreen(testDriver);
    }

    public ExploreScreen clickExplore() {
        exploreMenuItem().click();
        waiter.waitForDrawerToClose();
        return new ExploreScreen(testDriver);
    }

    public LikesScreen clickLikes() {
        likesMenuItem().click();
        waiter.waitForDrawerToClose();
        return new LikesScreen(testDriver);
    }

    public PlaylistScreen clickPlaylist() {
        playlistsMenuItem().click();
        waiter.waitForDrawerToClose();
        return new PlaylistScreen(testDriver);
    }
}
