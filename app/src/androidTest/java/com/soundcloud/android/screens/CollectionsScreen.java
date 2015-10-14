package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.CollectionsPlaylistOptionsDialogElement;

import android.support.v7.widget.RecyclerView;

public class CollectionsScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    public CollectionsScreen(Han solo) {
        super(solo);
    }

    public ViewElement emptyPlaylists() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.collections_empty_playlists)));
    }

    public CollectionsTrackLikesScreen clickTrackLikes() {
        trackLikesElement().click();
        return new CollectionsTrackLikesScreen(testDriver);
    }

    public ViewAllStationsScreen clickRecentStations() {
        recentStationsElement().click();
        return new ViewAllStationsScreen(testDriver);
    }

    public String getFirstPlaylistTitle() {
        return new TextElement(getFirstPlaylist().findElement(With.id(R.id.title))).getText();
    }

    public boolean isRecentStationsVisible() {
        return recentStationsElement().isVisible();
    }

    public PlaylistDetailsScreen clickOnPlaylist(With with) {
        collectionsView().scrollToItem(with).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    private ViewElement getFirstPlaylist() {
        return collectionsView().scrollToItem(With.id(R.id.collections_playlist_item));
    }

    public PlaylistDetailsScreen clickOnFirstPlaylist() {
        getFirstPlaylist().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public CollectionsPlaylistOptionsDialogElement clickPlaylistOptions() {
        playlistSettingsButton().click();
        return new CollectionsPlaylistOptionsDialogElement(testDriver);
    }

    public void removeFilters() {
        collectionsView().scrollToItem(With.id(R.id.btn_remove_filters)).click();
    }

    public int getLoadedItemCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return collectionsView().getItemCount();
    }

    public ViewElement playlistSettingsButton() {
        return testDriver.findElement(With.id(R.id.btn_collections_playlist_options));
    }

    private RecyclerViewElement collectionsView() {
        return testDriver.findElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    private ViewElement trackLikesElement() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.collections_your_liked_tracks)));
    }

    private ViewElement recentStationsElement() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.stations_collection_title_recently_played_stations)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
