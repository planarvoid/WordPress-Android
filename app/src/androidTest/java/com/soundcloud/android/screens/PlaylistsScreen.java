package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.playlists.PlaylistsActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.CollectionsPlaylistOptionsDialogElement;
import com.soundcloud.android.screens.elements.PlaylistElement;

import android.support.v7.widget.RecyclerView;

public class PlaylistsScreen extends Screen {

    protected static final Class ACTIVITY = PlaylistsActivity.class;

    public PlaylistsScreen(Han solo) {
        super(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
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
        ViewElement result = playlistsView().findOnScreenElement(
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

    private ViewElement playlistSettingsButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_collections_playlist_options));
    }

    public void removeFilters() {
        scrollToItem(With.id(R.id.btn_remove_filters)).click();
    }

    public CollectionScreen goBackToCollections() {
        testDriver.goBack();
        return new CollectionScreen(testDriver);
    }

    private RecyclerViewElement playlistsView() {
        waitForContentAndRetryIfLoadingFailed();
        return testDriver.findOnScreenElement(With.className(RecyclerView.class)).toRecyclerView();
    }

    public int visiblePlaylistsCount() {
        return testDriver.findOnScreenElements(With.id(R.id.collections_playlist_item)).size();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
