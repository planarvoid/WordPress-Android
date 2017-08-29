package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;
import static com.soundcloud.java.collections.Lists.transform;

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
import com.soundcloud.android.screens.profile.UserLikesScreen;
import com.soundcloud.android.screens.profile.UserPlaylistsScreen;
import com.soundcloud.android.screens.profile.UserRepostsScreen;
import com.soundcloud.android.screens.profile.UserTracksScreen;
import com.soundcloud.java.functions.Function;

import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.RecyclerView;

import java.util.List;

public class ProfileScreen extends Screen {

    /**
     * If the titles for the profile buckets ever change in the resource
     * strings, they will also have to change here. It seems like using
     * resources in tests is a bit unreliable, or doesn't work at all, which
     * is why we have this duplication here.
     */
    public enum Bucket {
        TRACKS("Tracks"),
        PLAYLISTS("Playlists"),
        ALBUMS("Albums"),
        REPOSTS("Reposts"),
        LIKES("Likes");

        private final String headerTitle;

        Bucket(String title) {
            this.headerTitle = title;
        }

        public String getHeaderTitle() {
            return this.headerTitle;
        }
    }

    public ProfileScreen(Han solo) {
        super(solo);
        waiter.waitForElementToBeVisible(userName());
        waiter.waitForElementToHaveText(new TextElement(userName()));
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public ProfileScreen(Han solo, String username) {
        super(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForElement(new TextElement(userName()), username);
    }

    public void goBack() {
        testDriver.goBack();
    }

    public ActivitiesScreen goBackToActivitiesScreen() {
        goBack();
        return new ActivitiesScreen(testDriver);
    }

    public TrackCommentsScreen goBackToTrackCommentsScreen() {
        goBack();
        return new TrackCommentsScreen(testDriver);
    }

    public VisualPlayerElement playTrack(int index) {
        VisualPlayerElement visualPlayerElement = getTracks().get(index).click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemElement trackWithTitle(final String title){
        ViewElement viewElement = scrollToItemInProfile(
                With.and(
                        With.id(R.id.track_list_item),
                        TrackItemElement.WithTitle(testDriver, title)));
        return new TrackItemElement(testDriver, viewElement);
    }

    public VisualPlayerElement playTrackWithTitle(final String title) {
        trackWithTitle(title).click();
        return new VisualPlayerElement(testDriver);
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        return new TrackItemElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.track_list_item)))
                .clickOverflowButton();
    }

    public ViewElement albumsHeader() {
        return scrollToItemInProfile(With.text(R.string.user_profile_sounds_header_albums));
    }

    public List<PlaylistElement> scrollToPlaylists() {
        // need to scroll to the playlist header first
        scrollToItemInProfile(With.text(R.string.user_profile_sounds_header_playlists));
        return playlists(com.soundcloud.android.R.id.playlist_list_item);
    }

    protected List<PlaylistElement> playlists(int withId) {
        return transform(
                testDriver.findOnScreenElements(With.id(withId)),
                toPlaylistItemElement
        );
    }

    private final Function<ViewElement, PlaylistElement> toPlaylistItemElement = viewElement -> PlaylistElement.forListItem(testDriver, viewElement);

    public String getFirstTrackTitle() {
        pullToRefresh();
        waiter.waitForContentAndRetryIfLoadingFailed();

        // 01/30/17: RecordTest showed failures in which api-mobile received timeouts when getting the Profile content from okidoki after uploading a track
        while (errorView().isOnScreen()) {
            pullToRefresh();
        }

        return getTracks().get(0).getTitle();
    }

    public int currentItemCount() {
        return currentRecyclerView().getItemCount();
    }

    private RecyclerViewElement currentRecyclerView() {
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    public TextElement bio() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.bio_text)));
    }

    public ProfileScreen touchInfoTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_info))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchSoundsTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_sounds))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    @Deprecated
    public ProfileScreen touchFollowingsTab() {
        final Tabs tabs = tabs();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        return this;
    }

    public FollowersScreen clickFollowersLink() {
        testDriver.findOnScreenElement(With.id(R.id.view_followers)).click();
        return new FollowersScreen(testDriver);
    }

    public FollowingsScreen clickFollowingsLink() {
        testDriver.findOnScreenElement(With.id(R.id.view_following)).click();
        return new FollowingsScreen(testDriver);
    }

    @Deprecated
    public ProfileScreen touchFollowersTab() {
        final Tabs tabs = tabs();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followers))).click();
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
        return testDriver.findOnScreenElement(With.id(R.id.tab_indicator_fixed)).toTabs();
    }

    private ViewElement followButton() {
        return testDriver.findOnScreenElement(With.id(R.id.toggle_btn_follow));
    }

    private ViewElement userName() {
        return testDriver.findOnScreenElement(With.id(R.id.username));
    }

    private String getActionFollowText() {
        return testDriver.getString(R.string.btn_follow);
    }

    private String getActionFollowingText() {
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
        waiter.waitForElementTextToChange(new TextElement(followButton()), getActionFollowText());
    }

    public void waitToNotBeFollowing() {
        waiter.waitForElementTextToChange(new TextElement(followButton()), getActionFollowText());
    }

    public boolean showsEmptySoundsMessage() {
        return testDriver.findOnScreenElement(With.text(R.string.empty_user_sounds_message)).hasVisibility();
    }

    public boolean areCurrentlyFollowing() {
        final String captionIfNotFollowing = getActionFollowText();
        final String currentCaption = new TextElement(followButton()).getText();
        return !currentCaption.equals(captionIfNotFollowing);
    }

    private void scrollToBucketAndClickFirstItem(final Bucket bucket, final int elementsId) {
        testDriver.scrollToFirstItemUnderHeader(With.and(With.text(bucket.getHeaderTitle()), With.id(R.id.sounds_header_text)), With.id(elementsId)).click();
    }

    public VisualPlayerElement scrollToBucketAndClickFirstTrack(final Bucket bucket) {
        scrollToBucketAndClickFirstItem(bucket, R.id.profile_user_sounds_track_row);

        final VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();

        return visualPlayer;
    }

    public PlaylistDetailsScreen scrollToBucketAndClickFirstPlaylist(final Bucket bucket) {
        scrollToBucketAndClickFirstItem(bucket, R.id.profile_user_sounds_playlist_row);

        return new PlaylistDetailsScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllTracks() {
        scrollToItemInProfile(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_tracks)));

        return this;
    }

    public UserTracksScreen goToAllTracks() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_tracks))).click();

        return new UserTracksScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllPlaylists() {
        scrollToItemInProfile(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_playlists)));

        return this;
    }

    public UserPlaylistsScreen goToAllPlaylists() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_playlists)))
                  .click();

        return new UserPlaylistsScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllReposts() {
        scrollToItemInProfile(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_reposts)));

        return this;
    }

    public UserRepostsScreen goToAllReposts() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_reposts))).click();

        return new UserRepostsScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllLikes() {
        scrollToItemInProfile(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_likes)));

        return this;
    }

    public UserLikesScreen goToAllLikes() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_likes))).click();

        return new UserLikesScreen(testDriver);
    }

    public String firstSocialLinkText() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.social_link))).getText();
    }

    private ViewElement scrollToItemInProfile(With with) {
        ViewElement profileCoordinator = testDriver.findOnScreenElement(With.id(R.id.profile_coordinator));
        profileCoordinator.findElement(With.className(AppBarLayout.class)).toAppBarLayout().collapse();
        return testDriver.scrollToItemInRecyclerView(with);
    }
}
