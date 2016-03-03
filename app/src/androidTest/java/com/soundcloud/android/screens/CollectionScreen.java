package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.CollectionsPlaylistOptionsDialogElement;
import com.soundcloud.android.screens.elements.LikedTracksPreviewElement;
import com.soundcloud.android.screens.elements.PlaylistElement;

import android.support.v7.widget.RecyclerView;

public class CollectionScreen extends Screen {

    protected static final Class ACTIVITY = MainActivity.class;

    public CollectionScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    public TrackLikesScreen clickLikedTracksPreview() {
        likedTracksPreviewElement().click();
        return new TrackLikesScreen(testDriver);
    }

    public ViewAllStationsScreen clickRecentStations() {
        recentStationsElement().click();
        return new ViewAllStationsScreen(testDriver);
    }

    public PlaylistElement scrollToPlaylistWithTitle(final String title) {
        ViewElement viewElement = scrollToItem(
                With.id(R.id.collections_playlist_item),
                PlaylistElement.WithTitle(testDriver, title)
        );
        return PlaylistElement.forCard(testDriver, viewElement);
    }

    public PlaylistDetailsScreen scrollToAndClickPlaylistWithTitle(String title) {
        return scrollToPlaylistWithTitle(title).click();
    }

    public PlaylistElement getPlaylistWithTitle(String title) {
        ViewElement result = collectionsView().findOnScreenElement(
                With.id(R.id.collections_playlist_item),
                PlaylistElement.WithTitle(testDriver, title)
        );
        return PlaylistElement.forCard(testDriver, result);
    }

    public PlaylistDetailsScreen clickOnFirstPlaylist() {
        scrollToFirstPlaylist().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public PlaylistElement scrollToFirstPlaylist() {
        ViewElement viewElement = scrollToItem(With.id(R.id.collections_playlist_item));
        return PlaylistElement.forCard(testDriver, viewElement);
    }

    public CollectionsPlaylistOptionsDialogElement clickPlaylistOptions() {
        playlistSettingsButton().click();
        return new CollectionsPlaylistOptionsDialogElement(testDriver);
    }

    public void removeFilters() {
        scrollToItem(With.id(R.id.btn_remove_filters)).click();
    }

    public int getLoadedItemCount() {
        return collectionsView().getItemCount();
    }

    public ViewElement playlistSettingsButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_collections_playlist_options));
    }

    private RecyclerViewElement collectionsView() {
        waitForContentAndRetryIfLoadingFailed();
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    public LikedTracksPreviewElement likedTracksPreviewElement() {
        final ViewElement viewElement = testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.collections_your_liked_tracks)));
        return new LikedTracksPreviewElement(testDriver, viewElement);
    }

    private ViewElement recentStationsElement() {
        return testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.stations_collection_title_recent_stations)));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
