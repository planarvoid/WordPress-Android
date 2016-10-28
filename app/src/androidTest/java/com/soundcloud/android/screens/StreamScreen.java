package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.StreamUpsellCardElement;
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
        streamList().scrollToBottom();
        return this;
    }

    public SearchScreen clickOnFindPeopleToFollow() {
        final With findPeopleToFollow = With.text(testDriver.getString(R.string.list_empty_stream_action));
        testDriver.findOnScreenElement(findPeopleToFollow).click();
        return new SearchScreen(testDriver);
    }

    public StreamCardElement scrollToFirstNotPromotedPlaylist() {
        return new StreamCardElement(testDriver, scrollToItem(
                With.id(R.id.playlist_list_item),
                PlaylistElement.NotPromoted(testDriver)
        ));
    }

    public PlaylistDetailsScreen clickFirstNotPromotedPlaylistCard() {
        return scrollToFirstNotPromotedPlaylist().clickToOpenPlaylist();
    }

    public StreamCardElement scrollToFirstTrack() {
        return new StreamCardElement(testDriver, scrollToItem(With.id(R.id.track_list_item)));
    }

    public StreamCardElement scrollToFirstSnippedTrack() {
        return new StreamCardElement(testDriver,
                                     scrollToItem(With.id(R.id.track_list_item),
                                                  StreamCardElement.WithGoIndicator(testDriver)));
    }

    public StreamUpsellCardElement scrollToUpsell() {
        return new StreamUpsellCardElement(testDriver, scrollToItem(With.id(R.id.stream_upsell)));
    }

    public VisualPlayerElement clickFirstTrackCard() {
        scrollToFirstTrack().clickToPlay();
        return new VisualPlayerElement(testDriver);
    }

    public VisualPlayerElement clickFirstNotPromotedTrackCard() {
        return scrollToFirstNotPromotedTrackCard().clickToPlay();
    }

    public StreamCardElement scrollToFirstNotPromotedTrackCard() {
        return new StreamCardElement(testDriver, scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.NotPromoted(testDriver)
        ));
    }

    public StreamCardElement scrollToFirstPlaylistTrackCard() {
        return new StreamCardElement(testDriver, scrollToItem(With.id(R.id.playlist_list_item)));
    }

    public StreamCardElement scrollToFirstRepostedTrack() {
        return new StreamCardElement(testDriver, scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.WithReposter(testDriver)
        ));
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        return scrollToFirstRepostedTrack().clickToPlay();
    }

    public TrackItemMenuElement clickFirstTrackCardOverflowButton() {
        return scrollToFirstTrack().clickOverflowButton();
    }

    public boolean isFirstTrackCardPromoted() {
        return scrollToFirstTrack().isPromotedTrack();
    }

    public boolean isPromotedTrackCardWithPromoter() {
        return scrollToFirstTrack().hasPromoter();
    }

    public PlaylistItemOverflowMenu clickFirstPlaylistOverflowButton() {
        scrollToFirstPlaylistTrackCard().clickOverflowButton();
        return new PlaylistItemOverflowMenu(testDriver);
    }

    public FacebookInvitesItemElement getFirstFacebookInvitesNotification() {
        return getFacebookInvitesNotifications().get(0);
    }

    public List<FacebookInvitesItemElement> getFacebookInvitesNotifications() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(
                testDriver.findOnScreenElements(With.id(R.id.facebook_invites_list_item)),
                toFacebookInvitesItemElement);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private RecyclerViewElement streamList() {
        return testDriver.findOnScreenElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private final Function<ViewElement, FacebookInvitesItemElement> toFacebookInvitesItemElement = new Function<ViewElement, FacebookInvitesItemElement>() {
        @Override
        public FacebookInvitesItemElement apply(ViewElement viewElement) {
            return new FacebookInvitesItemElement(testDriver, viewElement);
        }
    };
}
