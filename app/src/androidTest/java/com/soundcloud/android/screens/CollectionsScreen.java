package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.CollectionsPlaylistOptionsDialogElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;

import android.support.v7.widget.RecyclerView;

import java.util.List;

public class CollectionsScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    public CollectionsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    public TrackLikesScreen clickTrackLikes() {
        trackLikesElement().click();
        return new TrackLikesScreen(testDriver);
    }

    public ViewAllStationsScreen clickRecentStations() {
        recentStationsElement().click();
        return new ViewAllStationsScreen(testDriver);
    }

    public String getFirstPlaylistTitle() {
        return new TextElement(getFirstPlaylist().findElement(With.id(R.id.title))).getText();
    }

    public PlaylistDetailsScreen clickPlaylistWithTitle(String title) {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistItemElement(testDriver,
                collectionsView().scrollToFullyVisibleItem(With.text(title))).click();
        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();
        return playlistDetailsScreen;
    }

    private ViewElement getFirstPlaylist() {
        return collectionsView().scrollToFullyVisibleItem(With.id(R.id.collections_playlist_item));
    }

    public List<PlaylistItemElement> getPlaylists() {
        collectionsView().scrollToFullyVisibleItem(With.id(R.id.collections_playlist_item));
        return getPlaylists(R.id.collections_playlist_item);
    }

    public PlaylistDetailsScreen clickOnFirstPlaylist() {
        waiter.waitForContentAndRetryIfLoadingFailed();
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
        return collectionsView().getItemCount();
    }

    public ViewElement playlistSettingsButton() {
        return testDriver.findElement(With.id(R.id.btn_collections_playlist_options));
    }

    private RecyclerViewElement collectionsView() {
        waitForContentAndRetryIfLoadingFailed();
        return testDriver.findElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    private ViewElement trackLikesElement() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.collections_your_liked_tracks)));
    }

    private ViewElement recentStationsElement() {
        return testDriver.findElement(With.text(testDriver.getString(R.string.stations_collection_title_recent_stations)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
