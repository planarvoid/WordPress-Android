package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.RepeatableItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

import java.util.Collections;

class PlayQueueRecyclerItemAdapter
        extends RecyclerItemAdapter<PlayQueueUIItem, PlayQueueRecyclerItemAdapter.PlayQueueItemViewHolder>
        implements RepeatableItemAdapter {

    private static final int TRACK_ITEM_TYPE = 0;

    private PlayQueuePresenter.DragListener dragListener;

    @Inject
    public PlayQueueRecyclerItemAdapter(PlayQueueItemRenderer playQueueItemRenderer) {
        super(playQueueItemRenderer);
        setHasStableIds(true);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return TRACK_ITEM_TYPE;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getUniqueId();
    }

    @Override
    public void onBindViewHolder(final PlayQueueItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ImageView overflow = (ImageView) holder.itemView.findViewById(R.id.overflow_button);
        overflow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (dragListener != null) {
                    dragListener.startDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public void updateInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        for (int i = 0; i < items.size(); i++) {
            PlayQueueUIItem item = items.get(i);
            if (item.getRepeatMode() != repeatMode) {
                boolean shouldRerender = PlayQueueItemRenderer.shouldRerender(item.getRepeatMode(), repeatMode, item.getPlayState());
                item.setRepeatMode(repeatMode);
                if (shouldRerender) {
                    notifyItemChanged(i);
                }
            }
        }
    }


    public void updateNowPlaying(int position) {
        for (int i = 0; i < items.size(); i++) {
            setPlayState(position, i, items.get(i));
        }
        notifyDataSetChanged();
    }

    private void setPlayState(int position, int index, PlayQueueUIItem item) {
        if (position == index) {
            item.setPlayState(PlayQueueUIItem.PlayState.PLAYING);
        } else if (index > position) {
            item.setPlayState(PlayQueueUIItem.PlayState.COMING_UP);
        } else {
            item.setPlayState(PlayQueueUIItem.PlayState.PLAYED);
        }
    }

    void setDragListener(PlayQueuePresenter.DragListener dragListener) {
        this.dragListener = dragListener;
    }

    @Override
    public void removeItem(int position) {
        super.removeItem(position);
        notifyItemRemoved(position);
    }

    public void switchItems(int firstItemPosition, int secondItemPosition) {
        Collections.swap(items, firstItemPosition, secondItemPosition);
        notifyItemMoved(firstItemPosition, secondItemPosition);
    }

    @Override
    protected PlayQueueItemViewHolder createViewHolder(View itemView) {
        return new PlayQueueItemViewHolder(itemView);
    }

    public static class PlayQueueItemViewHolder extends RecyclerView.ViewHolder {
        public PlayQueueItemViewHolder(View itemView) {
            super(itemView);
        }
    }


}
