package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.viewelements.ViewNotFoundException;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    public StreamScreen(Han solo) {
        super(solo);
    }

    public int getItemCount() {
        return streamList().getItemCount();
    }

    public StreamScreen scrollToBottomOfPage() {
        streamList().scrollToBottomOfPage();
        return this;
    }

    public UpgradeScreen clickMidTierTrackForUpgrade(String title) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.textContaining(title)).click();
        return new UpgradeScreen(testDriver);
    }

    public PlaylistDetailsScreen clickFirstNotPromotedPlaylist() {
        int tries = 0;
        while (tries < MAX_SCROLLS_TO_FIND_ITEM) {
            streamList().scrollToItem(With.id(R.id.playlist_list_item));
            for (PlaylistItemElement playlistItemElement : getPlaylists()) {
                if (!playlistItemElement.isPromotedPlaylist()) {
                    return playlistItemElement.click();
                }
            }
            testDriver.scrollToBottom();
            tries++;
        }
        throw new ViewNotFoundException("Unable to find non-promoted playlist");
    }

    public StreamCardElement firstNotPromotedTrackCard() {
        if (!isFirstTrackPromoted()) {
            return firstTrackCard();
        }
        skipFirstItem();
        return firstTrackCard();
    }

    private void skipFirstItem() {
        streamList().scrollToPosition(1);
    }

    public StreamCardElement firstTrackCard() {
        return getTrackCard(0);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public StreamCardElement getTrackCard(int index) {
        return trackCardElements().get(index);
    }

    public TrackItemElement getTrack(int index) {
        return trackItemElements().get(index);
    }

    public VisualPlayerElement clickFirstTrack() {
        return clickTrack(0);
    }

    public VisualPlayerElement clickFirstNotPromotedTrack() {
        if (isFirstTrackPromoted()) {
            return clickTrack(1);
        } else {
            return clickFirstTrack();
        }
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        final ViewElement viewElement = streamList().scrollToItem(With.id(R.id.reposter));
        viewElement.click();
        VisualPlayerElement player = new VisualPlayerElement(testDriver);
        player.waitForExpandedPlayer();
        return player;
    }

    public VisualPlayerElement clickTrack(int index) {
        getTrack(index).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        getTrack(0).clickOverflowButton();
        return new TrackItemMenuElement(testDriver);
    }

    public PlaylistItemOverflowMenu clickFirstPlaylistOverflowButton() {
        getPlaylist(0).clickOverflow();
        return new PlaylistItemOverflowMenu(testDriver);
    }

    public boolean isFirstTrackPromoted() {
        return getTrack(0).isPromotedTrack();
    }

    public boolean isPromotedTrackWithPromoter() {
        return getTrack(0).hasPromoter();
    }

    public FacebookInvitesItemElement getFirstFacebookInvitesNotification() {
        return getFacebookInvitesNotifications().get(0);
    }

    public List<FacebookInvitesItemElement> getFacebookInvitesNotifications() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(
                testDriver.findElements(With.id(R.id.facebook_invites_list_item)),
                toFacebookInvitesItemElement);
    }

    private PlaylistItemElement getPlaylist(int index) {
        streamList().scrollToItem(With.id(R.id.playlist_list_item));
        return getPlaylists().get(index);
    }

    private RecyclerViewElement streamList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private List<StreamCardElement> trackCardElements() {
        streamList().scrollToItem(With.id(R.id.track_list_item));
        return Lists.transform(testDriver.findElements(With.id(R.id.track_list_item)), toTrackCardElements);
    }

    private List<TrackItemElement> trackItemElements() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamList().scrollToItem(With.id(R.id.track_list_item));
        return Lists.transform(testDriver.findElements(With.id(R.id.track_list_item)), toTrackItemElement);
    }

    private final Function<ViewElement, StreamCardElement> toTrackCardElements = new Function<ViewElement, StreamCardElement>() {
        @Override
        public StreamCardElement apply(ViewElement viewElement) {
            return new StreamCardElement(testDriver, viewElement);
        }
    };

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = new Function<ViewElement, TrackItemElement>() {
        @Override
        public TrackItemElement apply(ViewElement viewElement) {
            return new TrackItemElement(testDriver, viewElement);
        }
    };

    private final Function<ViewElement, FacebookInvitesItemElement> toFacebookInvitesItemElement = new Function<ViewElement, FacebookInvitesItemElement>() {
        @Override
        public FacebookInvitesItemElement apply(ViewElement viewElement) {
            return new FacebookInvitesItemElement(testDriver, viewElement);
        }
    };

}
