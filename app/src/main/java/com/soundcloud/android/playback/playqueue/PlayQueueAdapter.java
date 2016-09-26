package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.RepeatableItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Collections;

class PlayQueueAdapter extends RecyclerItemAdapter<PlayQueueUIItem, PlayQueueAdapter.PlayQueueItemViewHolder>
        implements RepeatableItemAdapter {

    private PlayQueuePresenter.DragListener dragListener;

    @Inject
    PlayQueueAdapter(TrackPlayQueueItemRenderer trackPlayQueueItemRenderer,
                     HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer) {
        super(new CellRendererBinding<>(PlayQueueUIItem.Kind.TRACK.ordinal(), trackPlayQueueItemRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.HEADER.ordinal(), headerPlayQueueItemRenderer)
        );
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
    public void onBindViewHolder(final PlayQueueItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ImageView overflow = (ImageView) holder.itemView.findViewById(R.id.overflow_button);

        if (overflow != null) {
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
    }

    @Override
    public void updateInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        for (int i = 0; i < items.size(); i++) {
            PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;

                if (trackItem.getRepeatMode() != repeatMode) {
                    boolean shouldRerender = TrackPlayQueueItemRenderer.shouldRerender(trackItem.getRepeatMode(),
                                                                                       repeatMode,
                                                                                       trackItem.getPlayState());
                    trackItem.setRepeatMode(repeatMode);
                    if (shouldRerender) {
                        notifyItemChanged(i);
                    }
                }
            }
        }
    }

    public void updateNowPlaying(int position) {
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                setPlayState(position, i, (TrackPlayQueueUIItem) item);
            }
        }
        notifyDataSetChanged();
    }

    private void setPlayState(int position, int index, TrackPlayQueueUIItem item) {
        if (position == index) {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.PLAYING);
        } else if (index > position) {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.COMING_UP);
        } else {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.PLAYED);
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

    void switchItems(int firstItemPosition, int secondItemPosition) {
        Collections.swap(items, firstItemPosition, secondItemPosition);
        notifyItemMoved(firstItemPosition, secondItemPosition);
    }

    @Override
    protected PlayQueueItemViewHolder createViewHolder(View itemView) {
        return new PlayQueueItemViewHolder(itemView);
    }

    static class PlayQueueItemViewHolder extends RecyclerView.ViewHolder {
        PlayQueueItemViewHolder(View itemView) {
            super(itemView);
        }
    }

    int getAdapterPosition(int queuePosition) {
        int cursor = 0;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isTrack()) {
                if (cursor == queuePosition) {
                    return i;
                }
                cursor++;
            }
        }
        return 0;
    }

    int getQueuePosition(int adapterPosition) {
        int cursor = 0;

        for (int i = 0; i < items.size(); i++) {
            if (i == adapterPosition) {
                return cursor;
            }

            if (items.get(i).isTrack()) {
                cursor++;
            }
        }
        return 0;
    }

}
