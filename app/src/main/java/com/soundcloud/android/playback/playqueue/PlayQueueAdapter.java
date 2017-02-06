package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Collections;

class PlayQueueAdapter extends RecyclerItemAdapter<PlayQueueUIItem, RecyclerItemAdapter.ViewHolder> {

    private final TrackPlayQueueItemRenderer trackRenderer;
    private final MagicBoxPlayQueueItemRenderer magicBoxRenderer;
            ;
    private PlayQueueView.DragListener dragListener;

    @Inject
    PlayQueueAdapter(TrackPlayQueueItemRenderer trackRenderer,
                     HeaderPlayQueueItemRenderer headerRenderer,
                     MagicBoxPlayQueueItemRenderer magicBoxRenderer) {
        super(new CellRendererBinding<>(PlayQueueUIItem.Kind.TRACK.ordinal(), trackRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.HEADER.ordinal(), headerRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.MAGIC_BOX.ordinal(), magicBoxRenderer)
        );
        this.trackRenderer = trackRenderer;
        this.magicBoxRenderer = magicBoxRenderer;
        setHasStableIds(true);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getUniqueId();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ImageView overflow = (ImageView) holder.itemView.findViewById(R.id.overflow_button);

        if (overflow != null) {
            overflow.setOnTouchListener((view, motionEvent) -> {
                if (dragListener != null) {
                    dragListener.startDrag(holder);
                }
                return false;
            });
        }
    }

    public void addItem(int position, PlayQueueUIItem playQueueUIItem) {
        getItems().add(position, playQueueUIItem);
        notifyItemInserted(position);
    }

    void setDragListener(PlayQueueView.DragListener dragListener) {
        this.dragListener = dragListener;
    }

    @Override
    public void removeItem(int position) {
        super.removeItem(position);
        notifyItemRemoved(position);
    }

    void switchItems(int firstItemPosition, int secondItemPosition) {
        Collections.swap(items, firstItemPosition, secondItemPosition);
        notifyItemMoved(firstItemPosition, secondItemPosition);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    void setTrackClickListener(TrackPlayQueueItemRenderer.TrackClickListener trackClickListener) {
        trackRenderer.setTrackClickListener(trackClickListener);
    }

    void setMagicBoxListener(MagicBoxPlayQueueItemRenderer.MagicBoxListener magicBoxListener) {
        magicBoxRenderer.setMagicBoxListener(magicBoxListener);
    }

}
