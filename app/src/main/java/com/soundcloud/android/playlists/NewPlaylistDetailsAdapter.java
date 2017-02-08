package com.soundcloud.android.playlists;

import static android.support.v4.view.MotionEventCompat.getActionMasked;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

@AutoFactory
class NewPlaylistDetailsAdapter extends RecyclerItemAdapter<PlaylistDetailItem, RecyclerView.ViewHolder> {

    private final PlaylistDetailView playlistDetailView;

    interface PlaylistDetailView {

        void onItemClicked(PlaylistDetailTrackItem trackItem);

        void onHandleTouched(RecyclerView.ViewHolder holder);

        void onUpsellItemDismissed(PlaylistDetailUpsellItem item);

        void onUpsellItemClicked(PlaylistDetailUpsellItem context);

        void onUpsellItemPresented();
    }

    NewPlaylistDetailsAdapter(PlaylistDetailView playlistDetailView,
                              @Provided PlaylistDetailTrackViewRenderer playlistDetailTrackViewRenderer,
                              @Provided PlaylistUpsellItemRenderer upsellItemRenderer,
                              @Provided PlaylistDetailOtherPlaylistsItemRenderer recommendationsItemRenderer) {
        super(new CellRendererBinding<>(PlaylistDetailItem.Kind.TrackItem.ordinal(), playlistDetailTrackViewRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.UpsellItem.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.OtherPlaylists.ordinal(), recommendationsItemRenderer));
        this.playlistDetailView = playlistDetailView;
        playlistDetailTrackViewRenderer.setListener(trackClickListener());
        upsellItemRenderer.setListener(upsellClickListener(playlistDetailView));
    }

    private UpsellItemRenderer.Listener upsellClickListener(final PlaylistDetailView playlistDetailView) {
        return new UpsellItemRenderer.Listener() {
            @Override
            public void onUpsellItemDismissed(int position) {
                playlistDetailView.onUpsellItemDismissed(upsellItem(position));
            }

            @Override
            public void onUpsellItemClicked(Context context, int position) {
                playlistDetailView.onUpsellItemClicked(upsellItem(position));
            }

            @Override
            public void onUpsellItemCreated() {
                playlistDetailView.onUpsellItemPresented();
            }
        };
    }

    private TrackItemRenderer.Listener trackClickListener() {
        return (urn, position) -> {
            final PlaylistDetailTrackItem item = trackItem(position);
            if (!item.inEditMode()) {
                playlistDetailView.onItemClicked(item);
            }
        };
    }

    private PlaylistDetailTrackItem trackItem(int position) {
        return (PlaylistDetailTrackItem) getItem(position);
    }

    private PlaylistDetailUpsellItem upsellItem(int position) {
        return (PlaylistDetailUpsellItem) getItem(position);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View itemView) {
        return new RecyclerView.ViewHolder(itemView) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        final ImageView overflow = PlaylistDetailTrackViewRenderer.ViewFetcher.handle(holder.itemView);
        if (overflow != null) {
            overflow.setOnTouchListener(createDragListener(holder));
        }
    }

    private View.OnTouchListener createDragListener(RecyclerView.ViewHolder holder) {
        return (view, motionEvent) -> {
            if (getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                playlistDetailView.onHandleTouched(holder);
            }
            return false;
        };
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getPlaylistItemKind().ordinal();
    }

}
