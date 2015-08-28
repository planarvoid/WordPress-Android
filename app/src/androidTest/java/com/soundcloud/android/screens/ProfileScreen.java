package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.strings.Strings;

import android.support.v7.widget.RecyclerView;
import android.widget.ListView;

import java.util.List;

public class ProfileScreen extends Screen {
    private static Class ACTIVITY = ProfileActivity.class;

    public ProfileScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement playTrack(int index) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        VisualPlayerElement visualPlayerElement = getTracks().get(index).click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public ProfileScreen clickUserAt(int index) {
        return getUsers().get(index).click();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    public PlaylistDetailsScreen clickFirstPlaylistWithTracks() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final List<PlaylistItemElement> playlists = getPlaylists();
        for (PlaylistItemElement playlistItemElement : playlists){
            final String trackCount = playlistItemElement.getTrackCount();
            if (Strings.isNotBlank(trackCount) && !trackCount.equalsIgnoreCase("0 tracks")) {
                return playlistItemElement.click();
            }
        }
        throw new IllegalStateException("Could not find playlist with a valid track count");
    }

    public String getFirstTrackTitle() {
        pullToRefresh();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getTracks().get(0).getTitle();
    }

    public void scrollToBottomOfCurrentListAndLoadMoreItems() {
        testDriver.scrollToBottom(currentList());
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void scrollToBottomOfCurrentRecyclerViewAndLoadMoreItems() {
        currentRecyclerView().scrollToBottomOfPage();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public int getCurrentListItemCount() {
        waiter.waitForItemCountToIncrease(currentList().getAdapter(), 0);
        return currentList().getAdapter().getCount();
    }

    public int getCurrentRecyclerViewItemCount() {
        waiter.waitForItemCountToIncrease(currentRecyclerView().getAdapter(), 0);
        return currentRecyclerView().getItemCount();
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final ViewElement viewElement = scrollToItem(With.id(R.id.reposter), currentRecyclerView());
        viewElement.click();
        return new VisualPlayerElement(testDriver);
    }

    private ListView currentList() {
        return (ListView) getViewPager().getCurrentPage(ListView.class);
    }

    private RecyclerViewElement currentRecyclerView() {
        return new RecyclerViewElement(testDriver.findElement(With.className(RecyclerView.class)), testDriver);
    }

    private ViewPagerElement getViewPager() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ViewPagerElement(testDriver);
    }

    public TextElement description() {
        return new TextElement(testDriver.findElement(With.id(R.id.description)));
    }

    public TextElement website() {
        return new TextElement(testDriver.findElement(With.id(R.id.website)));
    }

    public TextElement discogs() {
        return new TextElement(testDriver.findElement(With.id(R.id.discogs_name)));
    }

    public TextElement myspace() {
        return new TextElement(testDriver.findElement(With.id(R.id.myspace_name)));
    }

    public String emptyViewMessage() {
        return emptyView().message();
    }

    public ProfileScreen touchInfoTab() {
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_info).toUpperCase()).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchPlaylistsTab() {
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_playlists).toUpperCase()).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchLikesTab() {
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_likes).toUpperCase()).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowingsTab() {
        final SlidingTabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_likes).toUpperCase()).click();
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_followings).toUpperCase()).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowersTab() {
        final SlidingTabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_likes).toUpperCase()).click();
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_followers).toUpperCase()).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    private SlidingTabs tabs() {
        return testDriver.findElement(With.id(R.id.indicator)).toSlidingTabs();
    }

    private ViewElement followButton() {
        return testDriver.findElement(With.id(R.id.toggle_btn_follow));
    }

    private ViewElement userName() {
        return testDriver.findElement(With.id(R.id.username));
    }

    private ViewElement location() {
        return testDriver.findElement(With.id(R.id.location));
    }

    private ViewElement followersMessage() {
        return testDriver.findElement(With.id(R.id.followers_message));
    }

    private String getFollowButtonText() {
        return testDriver.getString(R.string.btn_following);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public String getUserName() {
        return new TextElement(userName()).getText();
    }

    public String getLocation() {
        return new TextElement(location()).getText();
    }

    public String getFollowersMessage() {
        return new TextElement(followersMessage()).getText();
    }

    public ProfileScreen clickFollowToggle() {
        followButton().click();
        return new ProfileScreen(testDriver);
    }

    public void waitToBeFollowing() {
        waiter.waitForElement(new TextElement(followButton()), getFollowButtonText());
    }

    public boolean isFollowButtonVisible() {
        return followButton().isVisible();
    }

    public boolean areCurrentlyFollowing() {
        final String captionIfFollowing = getFollowButtonText();
        final String currentCaption = new TextElement(followButton()).getText();
        return currentCaption.equals(captionIfFollowing);
    }

}
