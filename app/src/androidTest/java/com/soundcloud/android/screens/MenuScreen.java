package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.ToolBarElement;
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

    private ViewElement menuContainer() {
        return testDriver.findElement(With.id(R.id.nav_list));
    }

    protected ViewElement userProfileMenuItem() {
        return menuContainer().getChildAt(0);
    }

    private ViewElement streamMenuItem() {
        return menuContainer().findElement(With.text(testDriver.getString(R.string.side_menu_stream)));
    }

    protected ViewElement exploreMenuItem() {
        return menuContainer().findElement(With.text(testDriver.getString(R.string.side_menu_explore)));
    }

    protected ViewElement likesMenuItem() {
        return menuContainer().findElement(With.text(testDriver.getString(R.string.side_menu_likes)));
    }

    protected ViewElement playlistsMenuItem() {
        return menuContainer().findElement(With.text(testDriver.getString(R.string.side_menu_playlists)));
    }

    protected ViewElement upsellMenuItem() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.upsell_nav_body)));
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

    public boolean isClosed() {
        return !testDriver.isElementDisplayed(With.id(R.id.navigation_fragment_id));
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

    public PlaylistsScreen clickPlaylists() {
        playlistsMenuItem().click();
        waiter.waitForDrawerToClose();
        return new PlaylistsScreen(testDriver);
    }

    public UpgradeScreen clickUpsell() {
        upsellMenuItem().click();
        waiter.waitForDrawerToClose();
        return new UpgradeScreen(testDriver);
    }

}
