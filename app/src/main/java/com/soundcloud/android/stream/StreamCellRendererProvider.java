package com.soundcloud.android.stream;

import com.soundcloud.android.configuration.experiments.StreamDesignExperiment;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.profile.PostedPlaylistItemRenderer;
import com.soundcloud.android.profile.PostedTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import dagger.Lazy;

import javax.inject.Inject;

class StreamCellRendererProvider {

    private CellRenderer<TrackItem> trackItemRenderer;
    private CellRenderer<PlaylistItem> playlistItemRenderer;

    @Inject
    StreamCellRendererProvider(StreamDesignExperiment streamExperiment,
                               Lazy<PostedTrackItemRenderer> listTrackRenderer,
                               Lazy<PostedPlaylistItemRenderer> listPlaylistRenderer,
                               Lazy<StreamTrackItemRenderer> cardTrackRenderer,
                               Lazy<StreamPlaylistItemRenderer> cardPlaylistRenderer) {
        if (streamExperiment.isCardDesign()) {
            this.trackItemRenderer = cardTrackRenderer.get();
            this.playlistItemRenderer = cardPlaylistRenderer.get();
        } else {
            this.trackItemRenderer = listTrackRenderer.get();
            this.playlistItemRenderer = listPlaylistRenderer.get();
        }
    }

    public CellRenderer<TrackItem> getTrackItemRenderer() {
        return trackItemRenderer;
    }

    public CellRenderer<PlaylistItem> getPlaylistItemRenderer() {
        return playlistItemRenderer;
    }
}
