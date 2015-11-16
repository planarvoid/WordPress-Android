package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.screens.elements.StreamCardElement;
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

    public PlaylistItemElement firstNonPromotedPlaylist() {
        RecyclerViewElement.Criteria nonPromotedPlaylistCriteria = new RecyclerViewElement.Criteria() {
            @Override
            public boolean isSatisfied(ViewElement viewElement) {
                return !(viewElement instanceof EmptyViewElement) && !new PlaylistItemElement(testDriver, viewElement).isPromotedPlaylist();
            }
        };
        return new PlaylistItemElement(testDriver, streamList().scrollUntil(With.id(R.id.playlist_list_item), nonPromotedPlaylistCriteria));
    }

    public PlaylistDetailsScreen clickFirstNotPromotedPlaylist() {
        return firstNonPromotedPlaylist().click();
    }

    private void skipFirstItem() {
        streamList().scrollToPosition(streamList().lastBoundItemIndex());
    }

    public StreamCardElement firstTrackCard() {
        return trackCardElements().get(0);
    }

    public VisualPlayerElement clickFirstTrackCard() {
        return clickTrackCard(0);
    }

    public VisualPlayerElement clickFirstNotPromotedTrackCard() {
        return firstNotPromotedTrackCard().click();
    }

    public StreamCardElement firstNotPromotedTrackCard() {
        if (!isFirstTrackCardPromoted()) {
            return firstTrackCard();
        }
        skipFirstItem();
        return firstTrackCard();
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        ViewElement trackWithReposter = streamList()
                .scrollToItemWithChild(With.id(R.id.track_list_item), With.id(R.id.reposter));
        return new StreamCardElement(testDriver, trackWithReposter).click();
    }

    public VisualPlayerElement clickTrackCard(int index) {
        return trackCardElements().get(index).click();
    }

    public TrackItemMenuElement clickFirstTrackCardOverflowButton() {
        return firstTrackCard().clickOverflowButton();
    }

    public boolean isFirstTrackCardPromoted() {
        return firstTrackCard().isPromotedTrack();
    }

    public boolean isPromotedTrackCardWithPromoter(int index) {
        return trackCardElements().get(index).hasPromoter();
    }

    public PlaylistItemOverflowMenu clickFirstPlaylistOverflowButton() {
        return getPlaylist(0).clickOverflow();
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

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private PlaylistItemElement getPlaylist(int index) {
        streamList().scrollToFullyVisibleItem(With.id(R.id.playlist_list_item));
        return getPlaylists().get(index);
    }

    private RecyclerViewElement streamList() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private List<StreamCardElement> trackCardElements() {
        streamList().scrollToFullyVisibleItem(With.id(R.id.track_list_item));
        return Lists.transform(testDriver.findElements(With.id(R.id.track_list_item)), toTrackCardElements);
    }

    private final Function<ViewElement, StreamCardElement> toTrackCardElements = new Function<ViewElement, StreamCardElement>() {
        @Override
        public StreamCardElement apply(ViewElement viewElement) {
            return new StreamCardElement(testDriver, viewElement);
        }
    };

    private final Function<ViewElement, FacebookInvitesItemElement> toFacebookInvitesItemElement = new Function<ViewElement, FacebookInvitesItemElement>() {
        @Override
        public FacebookInvitesItemElement apply(ViewElement viewElement) {
            return new FacebookInvitesItemElement(testDriver, viewElement);
        }
    };

}
