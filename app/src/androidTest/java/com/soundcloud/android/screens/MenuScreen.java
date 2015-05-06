package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.elements.ListElement;
import com.soundcloud.android.screens.explore.ExploreScreen;

import java.util.List;

public class MenuScreen {
    private final ToolBarElement toolBar;
    protected Han testDriver;
    protected Waiter waiter;
    protected final int username_selector = R.id.username;

    public MenuScreen(Han solo) {
        this.testDriver = solo;
        this.toolBar = new ToolBarElement(solo);
        this.waiter = new Waiter(solo);
    }

    //TODO: Move this to systemSettingsScreen
    public SettingsScreen clickSystemSettings() {
        testDriver.findElement(With.text("Settings")).click();
        return new SettingsScreen(testDriver);
    }

    private ListElement menuContainer() {
        return testDriver.findElement(With.id(R.id.navigation_fragment_id)).toListView();
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
        toolBar.clickHomeButton();

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
