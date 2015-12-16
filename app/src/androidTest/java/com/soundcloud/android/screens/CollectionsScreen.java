package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.CollectionsPlaylistOptionsDialogElement;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

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
        collectionsView().scrollToTop();
        recentStationsElement().click();
        return new ViewAllStationsScreen(testDriver);
    }

    public ViewElement scrollToPlaylistWithTitle(String title) {
        return collectionsView().scrollToItem(new CollectionPlaylistWithTitleCriteria(testDriver, title));
    }

    public PlaylistElement getFirstPlaylist() {
        return scrollToFirstPlaylist();
    }

    public PlaylistDetailsScreen clickPlaylistWithTitle(String title) {
        PlaylistElement view = PlaylistElement.forCard(testDriver, scrollToPlaylistWithTitle(title));
        return view.click();
    }

    public PlaylistElement getPlaylist(String title) {
        final List<PlaylistElement> playlists = getPlaylists();

        for (PlaylistElement playlist : playlists) {
            if (title.equals(playlist.getTitle())) {
                return playlist;
            }
        }
        return PlaylistElement.forCard(testDriver, new EmptyViewElement("Playlist with title " + title));
    }

    public List<PlaylistElement> getPlaylists() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        // Item 3 on collections is Playlists
        collectionsView().scrollToItemAt(3);
        return getPlaylists(R.id.collections_playlist_item);
    }

    public PlaylistDetailsScreen clickOnFirstPlaylist() {
        scrollToFirstPlaylist().click();
        return new PlaylistDetailsScreen(testDriver);
    }

    protected List<PlaylistElement> getPlaylists(int withId) {
        return Lists.transform(
                testDriver.findElements(With.id(withId)),
                toPlaylistCardElement
        );
    }

    private final Function<ViewElement, PlaylistElement> toPlaylistCardElement = new Function<ViewElement, PlaylistElement>() {
        @Override
        public PlaylistElement apply(ViewElement viewElement) {
            return PlaylistElement.forCard(testDriver, viewElement);
        }
    };

    public PlaylistElement scrollToFirstPlaylist() {
        return PlaylistElement.forCard(testDriver, collectionsView().scrollToItem(new CollectionPlaylistCriteria()));
    }

    public CollectionsPlaylistOptionsDialogElement clickPlaylistOptions() {
        playlistSettingsButton().click();
        return new CollectionsPlaylistOptionsDialogElement(testDriver);
    }

    public void removeFilters() {
//        collectionsView().scrollToItem(With.id(R.id.btn_remove_filters)).click();
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


    private class CollectionPlaylistCriteria implements RecyclerViewElement.Criteria {

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return viewElement.getId() == R.id.collections_playlist_item;
        }

        @Override
        public String description() {
            return "IsPlaylist";
        }
    }

    private class CollectionPlaylistWithTitleCriteria implements RecyclerViewElement.Criteria {
        private final Han testDriver;
        private final String title;

        public CollectionPlaylistWithTitleCriteria(Han testDriver, String title) {
            this.testDriver = testDriver;
            this.title = title;
        }

        @Override
        public boolean isSatisfied(ViewElement viewElement) {
            return viewElement.getId() == R.id.collections_playlist_item && new StreamCardElement(testDriver, viewElement).trackTitle().equals(title);
        }

        @Override
        public String description() {
            return String.format("IsPlaylist, WithTitle: %s", title);
        }
    }
}
