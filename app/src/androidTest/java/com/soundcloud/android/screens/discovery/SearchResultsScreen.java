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
        waiter.waitForFragmentByTag(FRAGMENT);
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

    public ProfileScreen findAndClickFirstUserItem() {
        scrollToItem(With.id(com.soundcloud.android.R.id.user_list_item)).click();
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
        return searchTabs.goToTracksTab();
    }

    public SearchResultsScreen goToPlaylistsTab() {
        return searchTabs.goToPlaylistsTab();
    }

    public SearchResultsScreen goToPeopleTab() {
        return searchTabs.goToPeopleTab();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        testDriver.findElements(With.id(R.id.overflow_button)).get(0).click();
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

    protected List<PlaylistElement> getPlaylists(int withId) {
        return Lists.transform(
                testDriver.findElements(With.id(withId)),
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
        return testDriver.findElement(With.className(RecyclerView.class)).toRecyclerView();
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
