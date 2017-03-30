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

    public VisualPlayerElement playTrackWithTitle(final String title) {
        ViewElement viewElement = scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.WithTitle(testDriver, title)
        );
        new TrackItemElement(testDriver, viewElement).click();
        return new VisualPlayerElement(testDriver);
    }

    public ProfileScreen clickUserAt(int index) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getUsers().get(index).click();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        return new TrackItemElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.track_list_item)))
                .clickOverflowButton();
    }

    public PlaylistElement scrollToFirstPlaylist() {
        return PlaylistElement.forListItem(testDriver, scrollToItem(
                With.id(R.id.playlist_list_item)));
    }

    public ViewElement tracksHeader() {
        return scrollToItem(With.text(R.string.user_profile_sounds_header_tracks));
    }

    public ProfileScreen clickViewAllTracks() {
        scrollToItem(With.text(R.string.user_profile_sounds_view_all_tracks)).click();
        return this;
    }

    public ViewElement albumsHeader() {
        return scrollToItem(With.text(R.string.user_profile_sounds_header_albums));
    }

    public ProfileScreen clickViewAllAlbums() {
        scrollToItem(With.text(R.string.user_profile_sounds_view_all_albums)).click();
        return this;
    }

    public ViewElement repostHeader() {
        return scrollToItem(With.text(R.string.user_profile_sounds_header_reposts));
    }

    public ProfileScreen clickViewAllReposts() {
        scrollToItem(With.text(R.string.user_profile_sounds_view_all_reposts)).click();
        return this;
    }

    public ViewElement likesHeader() {
        return scrollToItem(With.text(R.string.user_profile_sounds_header_likes));
    }

    public ProfileScreen clickViewAllLikes() {
        scrollToItem(With.text(R.string.user_profile_sounds_view_all_likes)).click();
        return this;
    }

    public List<PlaylistElement> scrollToPlaylists() {
        // need to scroll to the playlist header first
        scrollToItem(With.text(R.string.user_profile_sounds_header_playlists));
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

    public ProfileScreen touchLegacyFollowingsTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_likes))).click();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ProfileScreen touchFollowingsTab() {
        final Tabs tabs = tabs();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        return this;
    }

    public ProfileScreen touchLegacyFollowersTab() {
        final Tabs tabs = tabs();
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_likes))).click();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followers))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

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
        return testDriver.findOnScreenElement(With.either(With.id(R.id.tab_indicator_scrollable), With.id(R.id.tab_indicator_fixed))).toTabs();
    }

    private ViewElement followButton() {
        return testDriver.findOnScreenElement(With.id(R.id.toggle_btn_follow));
    }

    public ViewElement artistStationButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_station));
    }

    public VisualPlayerElement clickArtistStationButton() {
        artistStationButton().click();
        return new VisualPlayerElement(testDriver);
    }

    private ViewElement userName() {
        return testDriver.findOnScreenElement(With.id(R.id.username));
    }

    private String getActionFollowText() {
        return testDriver.getString(R.string.btn_follow);
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

    public boolean showsEmptySoundsMessage() {
        return testDriver.findOnScreenElement(With.text(R.string.empty_user_sounds_message)).hasVisibility();
    }

    public boolean showsEmptyInfoMessage() {
        return testDriver.findOnScreenElement(With.text(R.string.info_empty_other_message)).hasVisibility();
    }

    public boolean showsEmptyFollowingsMessage() {
        return testDriver.findOnScreenElement(With.text(R.string.new_empty_user_followings_text)).hasVisibility();
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
        testDriver.scrollToItem(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_tracks)));

        return this;
    }

    public UserTracksScreen goToAllTracks() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_tracks))).click();

        return new UserTracksScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllPlaylists() {
        testDriver.scrollToItem(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_playlists)));

        return this;
    }

    public UserPlaylistsScreen goToAllPlaylists() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_playlists)))
                  .click();

        return new UserPlaylistsScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllReposts() {
        testDriver.scrollToItem(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_reposts)));

        return this;
    }

    public UserRepostsScreen goToAllReposts() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_reposts))).click();

        return new UserRepostsScreen(testDriver);
    }

    public ProfileScreen scrollToViewAllLikes() {
        testDriver.scrollToItem(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_likes)));

        return this;
    }

    public UserLikesScreen goToAllLikes() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.user_profile_sounds_view_all_likes))).click();

        return new UserLikesScreen(testDriver);
    }
}
