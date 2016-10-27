package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.TrackPlayQueueItemRenderer.shouldRerender;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Iterables.transform;
import static com.soundcloud.java.collections.ListsFunctions.cast;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Collections;

class PlayQueueAdapter extends RecyclerItemAdapter<PlayQueueUIItem, RecyclerItemAdapter.ViewHolder> {

    interface NowPlayingListener {
        void onNowPlayingChanged(TrackPlayQueueUIItem trackItem);
    }

    private final TrackPlayQueueItemRenderer trackPlayQueueItemRenderer;
    private final Iterable<TrackPlayQueueUIItem> tracksFromItems;
    private PlayQueuePresenter.DragListener dragListener;
    private NowPlayingListener nowPlayingListener;

    @Inject
    PlayQueueAdapter(TrackPlayQueueItemRenderer trackPlayQueueItemRenderer,
                     HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer) {
        super(new CellRendererBinding<>(PlayQueueUIItem.Kind.TRACK.ordinal(), trackPlayQueueItemRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.HEADER.ordinal(), headerPlayQueueItemRenderer)
        );
        this.trackPlayQueueItemRenderer = trackPlayQueueItemRenderer;
        tracksFromItems = transform(filter(items, TrackPlayQueueUIItem.IS_TRACK), cast(TrackPlayQueueUIItem.class));
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

    void updateInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        if (setRepeatMode(repeatMode)) {
            notifyDataSetChanged();
        }
    }

    private boolean setRepeatMode(PlayQueueManager.RepeatMode newRepeatMode) {
        boolean itemsChangedAndShouldRerender = false;
        for (TrackPlayQueueUIItem trackItem : tracksFromItems) {
            final PlayQueueManager.RepeatMode currentRepeatMode = trackItem.getRepeatMode();
            final TrackPlayQueueUIItem.PlayState trackPlayState = trackItem.getPlayState();
            final boolean shouldRerenderItem = shouldRerender(currentRepeatMode, newRepeatMode, trackPlayState);
            trackItem.setRepeatMode(newRepeatMode);

            itemsChangedAndShouldRerender = itemsChangedAndShouldRerender || shouldRerenderItem;
        }
        return itemsChangedAndShouldRerender;
    }

    public void updateNowPlaying(int position, boolean notifyListener) {

        if (items.size() == position && getItem(position).isTrack()
                && ((TrackPlayQueueUIItem) getItem(position)).getPlayState() == TrackPlayQueueUIItem.PlayState.PLAYING) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                setPlayState(position, i, trackItem, notifyListener);
            }
        }
        notifyDataSetChanged();
    }

    public void addItem(int position, PlayQueueUIItem playQueueUIItem) {
        getItems().add(position, playQueueUIItem);
        notifyDataSetChanged();
    }

    private void setPlayState(int position, int index, TrackPlayQueueUIItem item, boolean notifyListener) {
        if (position == index) {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.PLAYING);
            if (notifyListener && nowPlayingListener != null) {
                nowPlayingListener.onNowPlayingChanged(item);
            }
        } else if (index > position) {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.COMING_UP);
        } else {
            item.setPlayState(TrackPlayQueueUIItem.PlayState.PLAYED);
        }
    }

    void setNowPlayingChangedListener(NowPlayingListener nowPlayingListener) {
        this.nowPlayingListener = nowPlayingListener;
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
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    int getAdapterPosition(final PlayQueueItem currentPlayQueueItem) {
        return Iterables.indexOf(items, getPositionForItemPredicate(currentPlayQueueItem));
    }

    private static Predicate<PlayQueueUIItem> getPositionForItemPredicate(final PlayQueueItem currentPlayQueueItem) {
        return new Predicate<PlayQueueUIItem>() {
            @Override
            public boolean apply(PlayQueueUIItem input) {
                return input.isTrack() &&
                        ((TrackPlayQueueUIItem) input).getPlayQueueItem().equals(currentPlayQueueItem);
            }
        };
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

    void setTrackClickListener(TrackPlayQueueItemRenderer.TrackClickListener trackClickListener) {
        trackPlayQueueItemRenderer.setTrackClickListener(trackClickListener);
    }

}
