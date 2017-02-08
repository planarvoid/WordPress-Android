package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

class PlaylistDetailTrackViewRenderer implements CellRenderer<PlaylistDetailTrackItem> {
    private final PlaylistTrackItemRenderer playlistTrackItemRenderer;

    @Inject
    PlaylistDetailTrackViewRenderer(PlaylistTrackItemRendererFactory playlistTrackItemRendererFactory) {
        this.playlistTrackItemRenderer = playlistTrackItemRendererFactory.create(TrackItemMenuPresenter.RemoveTrackListener.EMPTY);
        this.playlistTrackItemRenderer.trackItemViewFactory().setLayoutId(R.layout.edit_playlist_track_item);
    }

    public void setListener(TrackItemRenderer.Listener listener) {
        playlistTrackItemRenderer.setListener(listener);
    }

    public View createItemView(ViewGroup parent) {
        return playlistTrackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailTrackItem> items) {
        final PlaylistDetailTrackItem detailTrackItem = items.get(position);
        playlistTrackItemRenderer.bindTrackView(position, itemView, detailTrackItem.trackItem());
        bindEditMode(itemView, detailTrackItem);
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
        ViewFetcher.handle(view).setVisibility(View.VISIBLE);
        ViewFetcher.overflow(view).setVisibility(View.GONE);
        ViewFetcher.preview(view).setVisibility(View.GONE);
        ViewFetcher.hideDuration(view);
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
