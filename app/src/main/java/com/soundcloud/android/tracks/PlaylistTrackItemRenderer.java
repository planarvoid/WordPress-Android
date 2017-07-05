package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistDetailTrackItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory(allowSubclasses = true)
public class PlaylistTrackItemRenderer implements CellRenderer<PlaylistDetailTrackItem> {

    private final TrackItemRenderer trackItemRenderer;
    private final RemoveTrackListener removeTrackListener;

    PlaylistTrackItemRenderer(RemoveTrackListener removeTrackListener,
                              @Provided TrackItemRenderer trackItemRenderer) {
        this.removeTrackListener = removeTrackListener;
        this.trackItemRenderer = trackItemRenderer;
        this.trackItemRenderer.trackItemViewFactory().setLayoutId(R.layout.edit_playlist_track_item);
    }

    public void setListener(TrackItemRenderer.Listener listener) {
        trackItemRenderer.setListener(listener);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailTrackItem> items) {
        PlaylistDetailTrackItem playlistDetailTrackItem = items.get(position);
        TrackItem trackItem = playlistDetailTrackItem.trackItem();
        trackItemRenderer.bindPlaylistTrackView(trackItem,
                                                itemView,
                                                position,
                                                Optional.of(playlistDetailTrackItem.playlistUrn()),
                                                createTrackSourceInfo(playlistDetailTrackItem, position),
                                                removeTrackListener);
    }

    private Optional<TrackSourceInfo> createTrackSourceInfo(PlaylistDetailTrackItem playlistDetailTrackItem, int position) {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.PLAYLIST_DETAILS.get(), true);
        trackSourceInfo.setOriginPlaylist(playlistDetailTrackItem.playlistUrn(), position, playlistDetailTrackItem.playlistOwnerUrn());
        playlistDetailTrackItem.promotedSourceInfo().ifPresent(trackSourceInfo::setPromotedSourceInfo);
        return Optional.of(trackSourceInfo);
    }
}
