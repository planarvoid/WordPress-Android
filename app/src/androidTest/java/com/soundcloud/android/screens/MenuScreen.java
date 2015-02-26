package com.soundcloud.android.screens;

import android.os.Build;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.ActionBarElement;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.explore.ExploreScreen;

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
        clickSystemSettings();
        testDriver.findElement(With.text(testDriver.getString(R.string.pref_revoke_access))).click();
        testDriver.assertText(R.string.menu_clear_user_title);
        testDriver.clickOnText(android.R.string.ok);
        return new HomeScreen(testDriver);
    }

    //TODO: Move this to systemSettingsScreen
    public SettingsScreen clickSystemSettings() {
        testDriver.findElement(With.text("Settings")).click();
        return new SettingsScreen(testDriver);
    }

    private ListElement menuContainer() {
        return testDriver.findElement(With.id(R.id.nav_listview)).toListView();
    }

    protected ViewElement userProfileMenuItem() {
        return menuContainer().getItemAt(0);
    }

    private ViewElement streamMenuItem() {
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
            testDriver.findElement(With.id(R.id.up)).click();
        }

        waiter.waitForDrawerToOpen();
        return new MenuScreen(testDriver);
    }

    public String getUserName() {
        return new TextElement(usernameLabel()).getText();
    }

    public boolean isOpened() {
        List<ViewElement> menuDrawers = testDriver.findElements(With.id(R.id.navigation_fragment_id));
        ViewElement menuDrawer = menuDrawers.isEmpty() ? null : menuDrawers.get(0);
        return menuDrawer != null && menuDrawer.isVisible();
    }

    public StreamScreen clickStream() {
        streamMenuItem().click();
        waiter.waitForDrawerToClose();
        return new StreamScreen(testDriver);
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

    public TrackLikesScreen clickLikes() {
        likesMenuItem().click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new TrackLikesScreen(testDriver);
    }

    public PlaylistsScreen clickPlaylist() {
        playlistsMenuItem().click();
        waiter.waitForDrawerToClose();
        return new PlaylistsScreen(testDriver);
    }
}
