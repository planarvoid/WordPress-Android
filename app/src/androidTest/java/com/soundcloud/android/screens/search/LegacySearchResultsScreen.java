package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.annotation.BrokenSearchTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.Tabs;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.search.LegacySearchActivity;

/**
 * This screen uses {@link LegacySearchActivity} and will be removed when getting rid
 * of all tests marked with {@link BrokenSearchTest}
 *
 * @deprecated use {@link SearchResultsScreen} instead.
 */
@Deprecated
public class LegacySearchResultsScreen extends Screen {
    private static final Class ACTIVITY = LegacySearchActivity.class;
    private static final String FRAGMENT = "tabbed_search";

    private static final String TRACKS_TAB_TEXT = "TRACKS";
    private static final String PLAYLISTS_TAB_TEXT = "PLAYLISTS";

    public LegacySearchResultsScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag(FRAGMENT);
    }

    public VisualPlayerElement clickFirstTrackItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public UpgradeScreen clickMidTierTrackForUpgrade(String name) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.textContaining(name)).click();
        return new UpgradeScreen(testDriver);
    }

    public ProfileScreen clickFirstUserItem() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.id(R.id.user_list_item)).click();
        return new ProfileScreen(testDriver);
    }

    public void pressBack() {
        testDriver.goBack();
    }

    public LegacySearchResultsScreen touchTracksTab() {
        touchTab(TRACKS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public LegacySearchResultsScreen touchPlaylistsTab() {
        touchTab(PLAYLISTS_TAB_TEXT);
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    private void touchTab(String tabText) {
        tabs().getTabWithText(tabText).click();
    }

    private Tabs tabs() {
        return testDriver.findElement(With.id(R.id.tab_indicator)).toTabs();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }
}
