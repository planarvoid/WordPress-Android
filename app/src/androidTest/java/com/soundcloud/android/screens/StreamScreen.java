package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.FacebookInvitesItemElement;
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
        streamList().scrollToBottom();
        return this;
    }

    public SearchScreen clickOnFindPeopleToFollow() {
        final With findPeopleToFollow = With.text(testDriver.getString(R.string.list_empty_stream_action));
        testDriver.findElement(findPeopleToFollow).click();
        return new SearchScreen(testDriver);
    }

    public UpgradeScreen clickMidTierTrackForUpgrade(String title) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.textContaining(title)).click();
        return new UpgradeScreen(testDriver);
    }

    public StreamCardElement scrollToFirstNotPromotedPlaylist() {
        return new StreamCardElement(testDriver, streamList().scrollToItem(new NotPromotedPlaylistCriteria(testDriver)));
    }

    public PlaylistDetailsScreen scrollToFirstNotPromotedPlaylistAndClickIt() {
        scrollToFirstNotPromotedPlaylist().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public StreamCardElement scrollToFirstTrack() {
        return new StreamCardElement(testDriver, streamList().scrollToItem(new TrackCriteria(testDriver)));
    }

    public VisualPlayerElement clickFirstTrackCard() {
        scrollToFirstTrack().click();
        return new VisualPlayerElement(testDriver);
    }

    public VisualPlayerElement clickFirstNotPromotedTrackCard() {
        return scrollToFirstNotPromotedTrackCard().click();
    }

    public StreamCardElement scrollToFirstNotPromotedTrackCard() {
        return new StreamCardElement(testDriver, streamList().scrollToItem(new NotPromotedTrackCriteria(testDriver)));
    }

    public StreamCardElement scrollToFirstPlaylistTrackCard() {
        return new StreamCardElement(testDriver,  streamList().scrollToItem(new PlaylistCriteria(testDriver)));
    }

    public StreamCardElement scrollToFirstRepostedTrack() {
        return new StreamCardElement(testDriver, streamList().scrollToItem(new RepostedTrackCriteria(testDriver)));
    }

    public VisualPlayerElement clickFirstRepostedTrack() {
        return scrollToFirstRepostedTrack().click();
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
                testDriver.findElements(With.id(R.id.facebook_invites_list_item)),
                toFacebookInvitesItemElement);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private RecyclerViewElement streamList() {
        return testDriver.findElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private final Function<ViewElement, FacebookInvitesItemElement> toFacebookInvitesItemElement = new Function<ViewElement, FacebookInvitesItemElement>() {
        @Override
        public FacebookInvitesItemElement apply(ViewElement viewElement) {
            return new FacebookInvitesItemElement(testDriver, viewElement);
        }
    };

    private abstract class StreamCardsCriteria {
        protected final Han testDriver;

        public StreamCardsCriteria(Han testDriver) {
            this.testDriver = testDriver;
        }

        protected StreamCardElement streamCardElement(ViewElement view) {
            return new StreamCardElement(testDriver, view);
        }

    }

    private class NotPromotedTrackCriteria extends StreamCardsCriteria implements ViewElement.Criteria {

        public NotPromotedTrackCriteria(Han testDriver) {
            super(testDriver);
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return streamCardElement(viewElement).isTrack() && !streamCardElement(viewElement).isPromotedTrack();
        }

        @Override
        public String description() {
            return "IsTrack, isNotPromoted";
        }
    }

    private class NotPromotedPlaylistCriteria extends StreamCardsCriteria implements ViewElement.Criteria {

        public NotPromotedPlaylistCriteria(Han testDriver) {
            super(testDriver);
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return streamCardElement(viewElement).isPlaylist() && !streamCardElement(viewElement).isPromotedTrack();
        }

        @Override
        public String description() {
            return "IsPlaylist, isNotPromoted";
        }
    }

    private class PlaylistCriteria extends StreamCardsCriteria implements ViewElement.Criteria {

        public PlaylistCriteria(Han testDriver) {
            super(testDriver);
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return streamCardElement(viewElement).isPlaylist();
        }

        @Override
        public String description() {
            return "IsPlaylist";
        }
    }

    private class TrackCriteria extends StreamCardsCriteria implements ViewElement.Criteria {

        public TrackCriteria(Han testDriver) {
            super(testDriver);
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return streamCardElement(viewElement).isTrack();
        }

        @Override
        public String description() {
            return "IsTrack";
        }
    }

    private class RepostedTrackCriteria extends StreamCardsCriteria implements ViewElement.Criteria {

        public RepostedTrackCriteria(Han testDriver) {
            super(testDriver);
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return streamCardElement(viewElement).isTrack() && streamCardElement(viewElement).hasReposter();
        }

        @Override
        public String description() {
            return "IsTrack, HasReposter";
        }
    }
}
