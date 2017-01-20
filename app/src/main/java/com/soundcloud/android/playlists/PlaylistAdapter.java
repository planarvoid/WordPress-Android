package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.android.view.dragdrop.RecyclerDragDropAdapter;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

@AutoFactory(allowSubclasses = true)
public class PlaylistAdapter
        extends RecyclerDragDropAdapter<PlaylistDetailItem, RecyclerDragDropAdapter.ViewHolder> {

    private final PlaylistUpsellItemRenderer upsellItemRenderer;

    PlaylistAdapter(OnStartDragListener dragListener,
                    PlaylistHeaderPresenter playlistHeaderPresenter,
                    PlaylistTrackItemRenderer playlistTrackItemRenderer,
                    @Provided PlaylistDetailTrackItemRendererFactory trackItemRenderer,
                    @Provided TrackEditItemRenderer editTrackItemRenderer,
                    @Provided PlaylistUpsellItemRenderer upsellItemRenderer,
                    @Provided PlaylistDetailOtherPlaylistsItemRenderer recommendationsItemRenderer) {
        super(dragListener,
              new CellRendererBinding<>(PlaylistDetailItem.Kind.TrackItem.ordinal(), trackItemRenderer.create(playlistTrackItemRenderer)),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.EditItem.ordinal(), editTrackItemRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.UpsellItem.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.HeaderItem.ordinal(), playlistHeaderPresenter),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.OtherPlaylists.ordinal(), recommendationsItemRenderer));
        this.upsellItemRenderer = upsellItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getPlaylistItemKind().ordinal();
    }

    @Override
    public void onNext(Iterable<PlaylistDetailItem> items) {
        clear();
        super.onNext(items);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public List<TrackItem> getTracks() {
        List<TrackItem> tracks = new ArrayList<>(getItems().size());
        for (PlaylistDetailItem item : getItems()) {
            if (item instanceof PlaylistDetailTrackItem) {
                tracks.add(((PlaylistDetailTrackItem) item).getTrackItem());
            }
        }
        return tracks;
    }

    void setOnUpsellClickListener(PlaylistUpsellItemRenderer.Listener listener) {
        this.upsellItemRenderer.setListener(listener);
    }
}
