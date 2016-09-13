package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.LikedTracksPreviewElement;
import com.soundcloud.android.screens.elements.PlaylistsPreviewElement;
import com.soundcloud.android.screens.stations.LikedStationsScreen;

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

    public PlaylistsScreen clickPlaylistsPreview() {
        likedPlaylistsPreviewElement().click();
        return new PlaylistsScreen(testDriver);
    }

    public LikedStationsScreen clickStations() {
        stationsElement().click();
        return new LikedStationsScreen(testDriver);
    }

    public LikedTracksPreviewElement likedTracksPreviewElement() {
        final ViewElement viewElement = testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.collections_your_liked_tracks)));
        return new LikedTracksPreviewElement(testDriver, viewElement);
    }

    private PlaylistsPreviewElement likedPlaylistsPreviewElement() {
        final ViewElement viewElement = testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.collections_playlists_header)));
        return new PlaylistsPreviewElement(testDriver, viewElement);
    }

    private ViewElement stationsElement() {
        return testDriver.findOnScreenElement(With.text(R.string.stations_collection_title_liked_stations));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
