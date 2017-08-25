package com.soundcloud.android.playlists;

import static android.support.v4.view.MotionEventCompat.getActionMasked;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import io.reactivex.Observable;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

@AutoFactory
class PlaylistDetailsAdapter extends PagingRecyclerItemAdapter<PlaylistDetailItem, RecyclerView.ViewHolder> {

    private final PlaylistDetailView playlistDetailView;
    private final Observable<PlaylistDetailTrackItem> trackItemClick;

    interface PlaylistDetailView {

        void onHandleTouched(RecyclerView.ViewHolder holder);

        void onUpsellItemDismissed(PlaylistDetailUpsellItem item);

        void onUpsellItemClicked(PlaylistDetailUpsellItem context);

        void onUpsellItemPresented();
    }

    PlaylistDetailsAdapter(PlaylistDetailView playlistDetailView,
                           PlaylistDetailsHeaderRenderer playlistDetailsHeaderRenderer,
                           @Provided PlaylistDetailsEmptyItemRenderer emptyItemRenderer,
                           @Provided PlaylistTrackItemRenderer playlistDetailTrackViewRenderer,
                           @Provided PlaylistUpsellItemRenderer upsellItemRenderer,
                           @Provided PlaylistDetailOtherPlaylistsItemRenderer recommendationsItemRenderer) {
        super(new CellRendererBinding<>(PlaylistDetailItem.Kind.HeaderItem.ordinal(), playlistDetailsHeaderRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.TrackItem.ordinal(), playlistDetailTrackViewRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.UpsellItem.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.OtherPlaylists.ordinal(), recommendationsItemRenderer),
              new CellRendererBinding<>(PlaylistDetailItem.Kind.EmptyItem.ordinal(), emptyItemRenderer));
        this.playlistDetailView = playlistDetailView;
        this.trackItemClick = playlistDetailTrackViewRenderer.trackItemClick();
        upsellItemRenderer.setListener(upsellClickListener(playlistDetailView));
    }

    Observable<PlaylistDetailTrackItem> trackItemClick() {
        return trackItemClick.filter(item -> !item.inEditMode());
    }

    private UpsellItemRenderer.Listener<PlaylistDetailUpsellItem> upsellClickListener(final PlaylistDetailView playlistDetailView) {
        return new UpsellItemRenderer.Listener<PlaylistDetailUpsellItem>() {
            @Override
            public void onUpsellItemDismissed(int position, PlaylistDetailUpsellItem item) {
                playlistDetailView.onUpsellItemDismissed(item);
            }

            @Override
            public void onUpsellItemClicked(Context context, int position, PlaylistDetailUpsellItem item) {
                playlistDetailView.onUpsellItemClicked(item);
            }

            @Override
            public void onUpsellItemCreated() {
                playlistDetailView.onUpsellItemPresented();
            }
        };
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View itemView) {
        return new RecyclerView.ViewHolder(itemView) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        final ImageView overflow = PlaylistTrackItemRenderer.ViewFetcher.handle(holder.itemView);
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
