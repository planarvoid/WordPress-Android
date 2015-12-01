package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.Tabs;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.strings.Strings;

import android.support.v7.widget.RecyclerView;

import java.util.List;

public class ProfileScreen extends Screen {

    public ProfileScreen(Han solo) {
        super(solo);
    }
    public ProfileScreen(Han solo, String username) {
        super(solo);
        waiter.waitForElement(new TextElement(userName()), username);
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

        return new TrackItemElement(testDriver, testDriver.findElement(With.id(R.id.track_list_item)))
                .clickOverflowButton();
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

    public void scrollToBottomAndLoadMoreItems() {
        currentRecyclerView().scrollToBottom();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public int currentItemCount() {
        return currentRecyclerView().getItemCount();
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        final ViewElement viewElement = currentRecyclerView().scrollToItem(new RecyclerViewElement.Criteria() {
                                                                               @Override
                                                                               public boolean isSatisfied(ViewElement viewElement) {
                                                                                   return viewElement.isElementDisplayed(With.id(R.id.reposter));
                                                                               }

                                                                               @Override
                                                                               public String description() {
                                                                                   return "HasReposter";
                                                                               }
                                                                           }
        );
        viewElement.click();
        VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();
        return visualPlayer;
    }

    private RecyclerViewElement currentRecyclerView() {
        return new RecyclerViewElement(testDriver.findElement(With.className(RecyclerView.class)), testDriver);
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
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_info)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchPlaylistsTab() {
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_playlists)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchLikesTab() {
        tabs().getTabWithText(testDriver.getString(R.string.tab_title_user_likes)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowingsTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_likes)).click();
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_followings)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowersTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_likes)).click();
        tabs.getTabWithText(testDriver.getString(R.string.tab_title_user_followers)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ExpandedProfileImageScreen touchProfileImage() {
        profileImage().click();
        return new ExpandedProfileImageScreen(testDriver);
    }

    private ViewElement profileImage() {
        return testDriver.findElement(With.id(R.id.image));
    }

    private Tabs tabs() {
        return testDriver.findElement(With.id(R.id.tab_indicator)).toTabs();
    }

    private ViewElement followButton() {
        return testDriver.findElement(With.id(R.id.toggle_btn_follow));
    }

    private ViewElement userName() {
        return testDriver.findElement(With.id(R.id.username));
    }

    private String getFollowButtonText() {
        return testDriver.getString(R.string.btn_following);
    }

    @Override
    protected Class getActivity() {
        return ProfileActivity.class;
    }

    public String getUserName() {
        return new TextElement(userName()).getText();
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
