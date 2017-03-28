package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.elements.AdapterElement;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.PlaylistUpsellCardElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class PlaylistDetailsScreen extends Screen {

    private static final Class ACTIVITY = PlaylistDetailActivity.class;

    public PlaylistDetailsScreen(Han solo) {
        super(solo);
    }

    public PlaylistsScreen goBackToPlaylists() {
        testDriver.goBack();
        return new PlaylistsScreen(testDriver);
    }

    public PlaylistDetailsScreen scrollToBottom() {
        getTracksContainer().scrollToBottom();
        return this;
    }

    public ConfirmDisableSyncCollectionScreen clickDownloadToDisableSyncCollection() {
        getDownloadToggle().click();
        return new ConfirmDisableSyncCollectionScreen(testDriver, MainActivity.class);
    }

    public PlaylistDetailsScreen clickDownloadToggle() {
        getDownloadToggle().click();
        return this;
    }

    public UpgradeScreen clickDownloadToggleForUpsell() {
        getDownloadToggle().click();
        return new UpgradeScreen(testDriver);
    }

    public PlaylistUpsellCardElement scrollToUpsell() {
        return new PlaylistUpsellCardElement(testDriver, scrollToItem(With.id(R.id.playlist_upsell)));
    }

    public ViewElement getDownloadToggle() {
        return testDriver
                .findOnScreenElement(With.id(R.id.toggle_download));
    }

    public PlaylistOverflowMenu clickPlaylistOverflowButton() {
        testDriver
                .findOnScreenElement(With.id(R.id.playlist_details_overflow_button))
                .click();
        return new PlaylistOverflowMenu(testDriver);
    }

    public DownloadImageViewElement headerDownloadElement() {
        return new DownloadImageViewElement(testDriver,
                                            testDriver.findOnScreenElement(With.id(R.id.header_download_state)));
    }

    public String getTitle() {
        return title().getText();
    }

    public boolean containsTrackWithTitle(String title) {
        return !(scrollToItem(
                With.id(R.id.track_list_item),
                TrackItemElement.WithTitle(testDriver, title)
        ) instanceof EmptyViewElement);
    }

    public VisualPlayerElement clickHeaderPlay() {
        headerPlayButton().click();
        waiter.waitForPlaybackToBePlaying();
        return new VisualPlayerElement(testDriver);
    }

    public void touchToggleLike() {
        likeToggle().click();
    }

    public VisualPlayerElement clickFirstTrack() {
        return scrollToAndGetFirstTrackItem().click();
    }

    public VisualPlayerElement clickTrack(int index) {
        VisualPlayerElement visualPlayerElement = getTracks()
                .get(index)
                .click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public String getFirstPlaylistInMoreTitle() {
        return scrollToAndGetFirstMorePlaylistsItem().getTitle();
    }

    public PlaylistDetailsScreen clickFirstPlaylistInMorePlaylists() {
        return scrollToAndGetFirstMorePlaylistsItem().click();
    }

    public VisualPlayerElement clickLastTrack() {
        return scrollToLastTrackItem()
                .trackItemElements()
                .get(trackItemElements().size() - 1)
                .click();
    }

    private TrackItemElement scrollToAndGetFirstTrackItem() {
        return toTrackItemElement.apply(scrollToItem(With.id(R.id.track_list_item)));
    }

    private PlaylistElement scrollToAndGetFirstMorePlaylistsItem() {
        return toPlaylistItemElement.apply(scrollToItem(With.id(R.id.carousel_playlist_item)));
    }

    private PlaylistDetailsScreen scrollToLastTrackItem() {
        testDriver.scrollListToLine(getTrackCount() - 1);
        return this;
    }

    public TrackItemMenuElement findAndClickFirstTrackOverflowButton() {
        return scrollToAndGetFirstTrackItem()
                .clickOverflowButton();
    }

    private void waitForDownloadToStart() {
        waiter.waitForElement(headerText(), offlineUpdateInProgress());
    }

    private TextElement headerText() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.header_text)));
    }

    public boolean waitForDownloadToFinish() {
        waitForDownloadToStart();
        return waiter.waitForNetworkCondition(() -> !testDriver.isElementDisplayed(With.text(offlineUpdateInProgress())));
    }

    private String offlineUpdateInProgress() {
        return testDriver.getString(R.string.offline_update_in_progress);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private TextElement title() {
        final ViewElement normalTitle = testDriver.findOnScreenElement(With.id(R.id.title));
        return normalTitle.hasVisibility() ? new TextElement(normalTitle) :
               new TextElement(testDriver.findOnScreenElement(With.id(R.id.title_private)));
    }

    private ViewElement headerPlayButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_play));
    }

    private ViewElement likeToggle() {
        return testDriver.findOnScreenElement(With.id(R.id.toggle_like));
    }

    private RecyclerViewElement tracksRecyclerView() {
        return testDriver
                .findOnScreenElement(With.id(R.id.ak_recycler_view))
                .toRecyclerView();
    }

    public int getTrackCount() {
        return getTracksContainer().getItemCount();
    }

    private List<TrackItemElement> trackItemElements() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return Lists.transform(
                getTracksContainer().findOnScreenElements(With.id(R.id.track_list_item)),
                toTrackItemElement
        );
    }

    private AdapterElement getTracksContainer() {
        return tracksRecyclerView();
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = viewElement -> new TrackItemElement(testDriver, viewElement);

    private final Function<ViewElement, PlaylistElement> toPlaylistItemElement = viewElement -> PlaylistElement.forCard(testDriver, viewElement);

    public void goBack() {
        testDriver.goBack();
    }
}
