package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistTrackItemRenderer implements CellRenderer<PlaylistDetailTrackItem> {

    private final TrackItemRenderer trackItemRenderer;
    private final TrackItemMenuPresenter.RemoveTrackListener removeTrackListener;

    @Inject
    PlaylistTrackItemRenderer(TrackItemRenderer trackItemRenderer) {
        this.removeTrackListener = TrackItemMenuPresenter.RemoveTrackListener.EMPTY;
        this.trackItemRenderer = trackItemRenderer;
        this.trackItemRenderer.trackItemViewFactory().setLayoutId(R.layout.edit_playlist_track_item);
    }


    void setListener(TrackItemRenderer.Listener listener) {
        trackItemRenderer.setListener(listener);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View itemView = trackItemRenderer.createItemView(parent);
        // this is because we set the list background to gray in playlists.
        // Should be in the layout when we do that everywhere
        itemView.setBackgroundResource(android.R.color.white);
        return itemView;
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
        bindEditMode(itemView, playlistDetailTrackItem);
    }

    private Optional<TrackSourceInfo> createTrackSourceInfo(PlaylistDetailTrackItem playlistDetailTrackItem, int position) {
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.PLAYLIST_DETAILS.get(), true);
        trackSourceInfo.setOriginPlaylist(playlistDetailTrackItem.playlistUrn(), position, playlistDetailTrackItem.playlistOwnerUrn());
        playlistDetailTrackItem.promotedSourceInfo().ifPresent(trackSourceInfo::setPromotedSourceInfo);
        return Optional.of(trackSourceInfo);
    }

    // TODO eventually move to a cell renderer
    private void bindEditMode(View itemView, PlaylistDetailTrackItem detailTrackItem) {
        if (detailTrackItem.inEditMode()) {
            bindHandle(itemView);
        } else {
            ViewFetcher.handle(itemView).setVisibility(View.GONE);
        }
    }

    private void bindHandle(View view) {
        ImageView handle = ViewFetcher.handle(view);
        handle.setVisibility(View.VISIBLE);
        ViewFetcher.overflow(view).setVisibility(View.GONE);
        ViewFetcher.preview(view).setVisibility(View.GONE);
        ViewFetcher.hideDuration(view);
        ViewUtils.extendTouchArea(handle, R.dimen.playlist_drag_handler_touch_extension);
    }


    static class ViewFetcher {

        static View preview(View itemView) {
            return ButterKnife.findById(itemView, R.id.preview_indicator);
        }

        static ImageView overflow(View itemView) {
            return ButterKnife.findById(itemView, R.id.overflow_button);
        }

        static ImageView handle(View itemView) {
            return ButterKnife.findById(itemView, R.id.drag_handle);
        }

        static void hideDuration(View itemView) {
            trackItemView(itemView).hideDuration();
        }

        static private TrackItemView trackItemView(View itemView) {
            return (TrackItemView) itemView.getTag();
        }

    }
}
