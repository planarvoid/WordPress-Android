package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.SearchTabs;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.support.v7.widget.RecyclerView;

import java.util.List;

public class SearchResultsScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;
    private static final String FRAGMENT = "tabbed_search";

    private final SearchTabs searchTabs;

    public SearchResultsScreen(Han solo) {
        super(solo);
        this.searchTabs = new SearchTabs(solo);
        waiter.assertForFragmentByTag(FRAGMENT);
    }

    public VisualPlayerElement findAndClickFirstTrackItem() {
        scrollToItem(With.id(com.soundcloud.android.R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public PlaylistDetailsScreen findAndClickFirstPlaylistItem() {
        scrollToItem(With.id(R.id.playlist_list_item)).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistDetailsScreen findAndClickFirstAlbumItem() {
        findAndClickFirstPlaylistItem();
        return new PlaylistDetailsScreen(testDriver);
    }

    public ProfileScreen findAndClickFirstUserItem() {
        scrollToItem(With.id(R.id.user_list_item)).click();
        return new ProfileScreen(testDriver);
    }

    public int getResultItemCount() {
        final RecyclerViewElement recyclerViewElement = resultsList();
        waiter.waitForItemCountToIncrease(recyclerViewElement.getAdapter(), 0);
        return recyclerViewElement.getItemCount();
    }

    public String currentTabTitle() {
        return getViewPager().getCurrentTabText();
    }

    public SearchResultsScreen scrollToBottomOfTracksListAndLoadMoreItems() {
        resultsList().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public SearchResultsScreen goToTracksTab() {
        return searchTabs.swipeLeftToTracksTab();
    }

    public SearchResultsScreen goToPlaylistsTab() {
        return searchTabs.swipeLeftToPlaylistsTab();
    }

    public SearchResultsScreen goToAlbumsTab() {
        return searchTabs.swipeLeftToAlbumsTab();
    }

    public SearchResultsScreen goToPeopleTab() {
        return searchTabs.swipeLeftToPeopleTab();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        testDriver.findOnScreenElements(With.id(R.id.overflow_button)).get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    public DiscoveryScreen goBack() {
        testDriver.goBack();
        return new DiscoveryScreen(testDriver);
    }

    public List<PlaylistElement> getPlaylists() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getPlaylists(com.soundcloud.android.R.id.playlist_list_item);
    }

    public SearchPremiumResultsScreen clickOnViewPremiumContent() {
        premiumContent().findOnScreenElement(With.id(R.id.view_all_container)).click();
        return new SearchPremiumResultsScreen(testDriver);
    }

    public VisualPlayerElement clickOnPremiumContent() {
        premiumContent().findOnScreenElement(With.id(R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public UpgradeScreen clickOnPremiumContentHelp() {
        premiumContent().findOnScreenElement(With.id(R.id.help)).click();
        return new UpgradeScreen(testDriver);
    }

    protected List<PlaylistElement> getPlaylists(int withId) {
        return Lists.transform(
                testDriver.findOnScreenElements(With.id(withId)),
                toPlaylistItemElement
        );
    }

    private final Function<ViewElement, PlaylistElement> toPlaylistItemElement = new Function<ViewElement, PlaylistElement>() {
        @Override
        public PlaylistElement apply(ViewElement viewElement) {
            return PlaylistElement.forListItem(testDriver, viewElement);
        }
    };

    private ViewPagerElement getViewPager() {
        return new ViewPagerElement(testDriver);
    }

    private RecyclerViewElement resultsList() {
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    private ViewElement premiumContent() {
        return testDriver.findOnScreenElement(With.id(R.id.premium_item_container));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }

    public boolean premiumContentIsOnScreen() {
        return premiumContent().isOnScreen();
    }
}
