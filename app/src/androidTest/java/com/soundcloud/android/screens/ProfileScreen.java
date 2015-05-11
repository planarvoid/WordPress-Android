package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.SlidingTabs;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.widget.ListView;

import java.util.List;

public class ProfileScreen extends Screen {
    private static Class ACTIVITY = ProfileActivity.class;

    public ProfileScreen(Han solo) {
        super(solo);
    }

    public VisualPlayerElement playTrack(int index) {
        waiter.waitForElements(R.id.track_list_item);
        tracks().get(index).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver
                .findElements(With.id(R.id.overflow_button))
                .get(0).click();
        return new TrackItemMenuElement(testDriver);
    }

    public String getFirstTrackTitle() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        TextElement textElement = new TextElement(testDriver.findElements(With.id(R.id.list_item_subheader)).get(0));
        return textElement.getText();
    }

    public void scrollToBottomOfCurrentListAndLoadMoreItems() {
        testDriver.scrollToBottom(currentList());
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public int getCurrentListItemCount() {
        waiter.waitForItemCountToIncrease(currentList().getAdapter(), 0);
        return currentList().getAdapter().getCount();
    }

    private ListView currentList() {
        return (ListView) getViewPager().getCurrentPage(ListView.class);
    }

    private ViewPagerElement getViewPager() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return new ViewPagerElement(testDriver);
    }

    public ViewElement emptyUserPostsMessage(String username){
        return emptyView().findElement(With.text(testDriver.getString(R.string.empty_user_tracks_text, username)));
    }

    public ViewElement emptyUserLikesMessage(String username){
        return emptyView().findElement(With.text(testDriver.getString(R.string.empty_user_likes_text, username)));
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

    private SlidingTabs tabs() {
        return testDriver.findElement(With.id(R.id.indicator)).toSlidingTabs();
    }

    private List<ViewElement> tracks() {
        return testDriver.findElements(With.id(R.id.track_list_item));
    }

    private List<ViewElement> playlists() {
        return testDriver.findElements(With.id(R.id.playlist_list_item));
    }

    private List<ViewElement> users() {
        return testDriver.findElements(With.id(R.id.user_list_item));
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

    private ViewElement followersMessage(){
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
