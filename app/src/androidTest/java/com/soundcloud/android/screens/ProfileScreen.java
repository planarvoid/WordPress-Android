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
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public ProfileScreen(Han solo, String username) {
        super(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForElement(new TextElement(userName()), username);
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
        return getUsers().get(index).click();
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {

        return new TrackItemElement(testDriver, testDriver.findOnScreenElement(With.id(R.id.track_list_item)))
                .clickOverflowButton();
    }

    public PlaylistDetailsScreen clickFirstPlaylistWithTracks() {
        final List<PlaylistElement> playlists = getPlaylists();
        for (PlaylistElement playlistElement : playlists) {
            final String trackCount = playlistElement.getTrackCount();
            if (Strings.isNotBlank(trackCount) && !trackCount.equalsIgnoreCase("0 tracks")) {
                return playlistElement.click();
            }
        }
        throw new IllegalStateException("Could not find playlist with a valid track count");
    }

    public PlaylistElement scrollToFirstPlaylist() {
        return PlaylistElement.forListItem(testDriver, scrollToItem(
                With.id(R.id.playlist_list_item)));
    }

    public void scrollToReposts() {
        scrollToItem(With.id(R.id.track_list_item));
    }

    public ViewElement getSpotlightTitle() {
        return scrollToItem(With.text(R.string.user_profile_sounds_header_spotlight));
    }

    public void clickViewAllTracks() {
        scrollToItem(With.text(R.string.user_profile_sounds_view_all_tracks)).click();
    }

    public List<PlaylistElement> getPlaylists() {
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

    public static With WithProfileHeader(final Han testDriver, final int headerTitle) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return view.findOnScreenElement(
                        With.id(R.id.sounds_header_text),
                        With.text(headerTitle)
                );
            }

            @Override
            public String getSelector() {
                return "Header profile item with title " + headerTitle;
            }
        };
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
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
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
        // TODO we have to go to the middle to even see the next tab. tabs should scroll as necessary
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followers))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public ExpandedProfileImageScreen touchProfileImage() {
        profileImage().click();
        return new ExpandedProfileImageScreen(testDriver);
    }

    public ProfileScreen touchSoundsTab() {
        tabs().getTabWith(text(testDriver.getString(R.string.tab_title_user_sounds))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }

    public VisualPlayerElement testttt() {
        ViewElement repostHeader = testDriver.scrollToItem(
                With.id(R.id.user_sounds_list_item),
                WithProfileHeader(testDriver, R.string.user_profile_sounds_header_reposts)
        );
        List<ViewElement> viewElements = testDriver.findOnScreenElements(With.id(R.id.user_sounds_list_item));
        int indexOfHeader = viewElements.indexOf(repostHeader);
        List<ViewElement> afterHeader = viewElements.subList(indexOfHeader, viewElements.size());

        for (ViewElement ve : afterHeader) {
            ViewElement trackListElement = ve.findElement(With.id(R.id.track_list_item));
            if (trackListElement != null) {
                trackListElement.click();
                break;
            }
        }

        VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();
        return visualPlayer;
    }

    private ViewElement profileImage() {
        return testDriver.findOnScreenElement(With.id(R.id.image));
    }

    Tabs tabs() {
        return testDriver.findOnScreenElement(With.id(R.id.tab_indicator)).toTabs();
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

    public boolean isFollowButtonVisible() {
        return followButton().isOnScreen();
    }

    public boolean areCurrentlyFollowing() {
        final String captionIfNotFollowing = getActionFollowText();
        final String currentCaption = new TextElement(followButton()).getText();
        return !currentCaption.equals(captionIfNotFollowing);
    }

}
