package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.TrackPlayQueueItemRenderer.shouldRerender;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

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
    private PlayQueuePresenter.DragListener dragListener;
    private NowPlayingListener nowPlayingListener;

    @Inject
    PlayQueueAdapter(TrackPlayQueueItemRenderer trackPlayQueueItemRenderer,
                     HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer) {
        super(new CellRendererBinding<>(PlayQueueUIItem.Kind.TRACK.ordinal(), trackPlayQueueItemRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.HEADER.ordinal(), headerPlayQueueItemRenderer)
        );
        this.trackPlayQueueItemRenderer = trackPlayQueueItemRenderer;
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
        for (PlayQueueUIItem item : items) {
            if (item.isTrack()) {
                TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                final PlayQueueManager.RepeatMode currentRepeatMode = trackItem.getRepeatMode();
                final PlayState trackPlayState = trackItem.getPlayState();
                final boolean shouldRerenderItem = shouldRerender(currentRepeatMode, newRepeatMode, trackPlayState);
                trackItem.setRepeatMode(newRepeatMode);
                itemsChangedAndShouldRerender = itemsChangedAndShouldRerender || shouldRerenderItem;
            } else if (item.isHeader()) {
                HeaderPlayQueueUIItem headItem = (HeaderPlayQueueUIItem) item;
                headItem.setRepeatMode(newRepeatMode);
            }
        }
        return itemsChangedAndShouldRerender;
    }

    public void updateNowPlaying(int position, boolean notifyListener) {

        if (items.size() == position && getItem(position).isTrack()
                && getItem(position).getPlayState() == PlayState.PLAYING) {
            return;
        }

        Optional<HeaderPlayQueueUIItem> lastHeaderPlayQueueUiItem = Optional.absent();
        boolean headerPlayStateSet = false;
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                setPlayState(position, i, trackItem, notifyListener);
                if (!headerPlayStateSet && lastHeaderPlayQueueUiItem.isPresent()) {
                    headerPlayStateSet = shouldAddHeader(trackItem);
                    lastHeaderPlayQueueUiItem.get().setPlayState(trackItem.getPlayState());
                }
            } else if (item.isHeader()) {
                lastHeaderPlayQueueUiItem = Optional.of((HeaderPlayQueueUIItem) item);
                headerPlayStateSet = false;
            }
        }
        notifyDataSetChanged();
    }

    public void addItem(int position, PlayQueueUIItem playQueueUIItem) {
        getItems().add(position, playQueueUIItem);
        notifyDataSetChanged();
    }

    private void setPlayState(int currentlyPlayingPosition, int itemPosition, TrackPlayQueueUIItem item, boolean notifyListener) {
        if (currentlyPlayingPosition == itemPosition) {
            item.setPlayState(PlayState.PLAYING);
            if (notifyListener && nowPlayingListener != null) {
                nowPlayingListener.onNowPlayingChanged(item);
            }
        } else if (itemPosition > currentlyPlayingPosition) {
            item.setPlayState(PlayState.COMING_UP);
        } else {
            item.setPlayState(PlayState.PLAYED);
        }
    }

    private boolean shouldAddHeader(TrackPlayQueueUIItem trackItem) {
        return trackItem.getPlayState() == PlayState.PLAYING ||
                trackItem.getPlayState() == PlayState.COMING_UP;
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
