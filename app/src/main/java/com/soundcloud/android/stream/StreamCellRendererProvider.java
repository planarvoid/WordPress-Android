package com.soundcloud.android.stream;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.PlayingTrackRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import dagger.Lazy;

import javax.inject.Inject;

class StreamCellRendererProvider {

    private PlayingTrackRenderer trackItemRenderer;
    private CellRenderer<PlaylistItem> playlistItemRenderer;

    @Inject
    StreamCellRendererProvider(FeatureFlags featureFlags,
                               Lazy<TrackItemRenderer> listTrackRenderer,
                               Lazy<PlaylistItemRenderer> listPlaylistRenderer,
                               Lazy<StreamTrackItemRenderer> cardTrackRenderer,
                               Lazy<StreamPlaylistItemRenderer> cardPlaylistRenderer) {
        if (featureFlags.isEnabled(Flag.NEW_STREAM)) {
            this.trackItemRenderer = cardTrackRenderer.get();
            this.playlistItemRenderer = cardPlaylistRenderer.get();
        } else {
            this.trackItemRenderer = listTrackRenderer.get();
            this.playlistItemRenderer = listPlaylistRenderer.get();
        }
    }

    public PlayingTrackRenderer getTrackItemRenderer() {
        return trackItemRenderer;
    }

    public CellRenderer<PlaylistItem> getPlaylistItemRenderer() {
        return playlistItemRenderer;
    }
}
