package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.Tabs;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
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

        VisualPlayerElement visualPlayerElement = getTracks().get(index).click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public ProfileScreen clickUserAt(int index) {
        return getUsers().get(index).click();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        waiter.waitForContentAndRetryIfLoadingFailed();

        return new TrackItemElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.track_list_item)))
                .clickOverflowButton();
    }

    public PlaylistDetailsScreen clickFirstPlaylistWithTracks() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final List<PlaylistElement> playlists = getPlaylists();
        for (PlaylistElement playlistElement : playlists) {
            final String trackCount = playlistElement.getTrackCount();
            if (Strings.isNotBlank(trackCount) && !trackCount.equalsIgnoreCase("0 tracks")) {
                return playlistElement.click();
            }
        }
        throw new IllegalStateException("Could not find playlist with a valid track count");
    }

    public List<PlaylistElement> getPlaylists() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getPlaylists(com.soundcloud.android.R.id.playlist_list_item);
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
        final ViewElement viewElement = scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.WithReposter(testDriver)
        );

        viewElement.click();
        VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();
        return visualPlayer;
    }

    private RecyclerViewElement currentRecyclerView() {
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    public TextElement description() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.description)));
    }

    public TextElement website() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.website)));
    }

    public TextElement discogs() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.discogs_name)));
    }

    public TextElement myspace() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.myspace_name)));
    }

    public String emptyViewMessage() {
        return emptyView().message();
    }

    public ProfileScreen touchInfoTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_info))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchPlaylistsTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_playlists))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchLikesTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_likes))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowingsTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_likes))).click();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowersTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_likes))).click();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followers))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ExpandedProfileImageScreen touchProfileImage() {
        profileImage().click();
        return new ExpandedProfileImageScreen(testDriver);
    }

    private ViewElement profileImage() {
        return testDriver.findOnScreenElement(With.id(R.id.image));
    }

    private Tabs tabs() {
        return testDriver.findOnScreenElement(With.id(R.id.tab_indicator)).toTabs();
    }

    private ViewElement followButton() {
        return testDriver.findOnScreenElement(With.id(R.id.toggle_btn_follow));
    }

    private ViewElement userName() {
        return testDriver.findOnScreenElement(With.id(R.id.username));
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
        return followButton().isOnScreen();
    }

    public boolean areCurrentlyFollowing() {
        final String captionIfFollowing = getFollowButtonText();
        final String currentCaption = new TextElement(followButton()).getText();
        return currentCaption.equals(captionIfFollowing);
    }

}
