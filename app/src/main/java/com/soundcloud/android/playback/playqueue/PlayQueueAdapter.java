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

import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Collections;

class PlayQueueAdapter extends RecyclerItemAdapter<PlayQueueUIItem, RecyclerItemAdapter.ViewHolder> {

    interface NowPlayingListener {
        void onNowPlayingChanged(TrackPlayQueueUIItem trackItem);
    }

    private final TrackPlayQueueItemRenderer trackRenderer;
    private PlayQueuePresenter.DragListener dragListener;
    private NowPlayingListener nowPlayingListener;

    @Inject
    PlayQueueAdapter(TrackPlayQueueItemRenderer trackRenderer,
                     HeaderPlayQueueItemRenderer headerRenderer,
                     MagicBoxPlayQueueItemRenderer magicBoxRenderer) {
        super(new CellRendererBinding<>(PlayQueueUIItem.Kind.TRACK.ordinal(), trackRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.HEADER.ordinal(), headerRenderer),
              new CellRendererBinding<>(PlayQueueUIItem.Kind.MAGIC_BOX.ordinal(), magicBoxRenderer)
        );
        this.trackRenderer = trackRenderer;
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

    void updateInRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        if (setRepeatMode(repeatMode)) {
            notifyDataSetChanged();
        }
    }

    private boolean setRepeatMode(PlayQueueManager.RepeatMode newRepeatMode) {
        boolean itemsChangedAndShouldRerender = false;

        for (PlayQueueUIItem item : items) {
            final PlayQueueManager.RepeatMode currentRepeatMode = item.getRepeatMode();
            final PlayState trackPlayState = item.getPlayState();
            final boolean shouldRerenderItem = shouldRerender(currentRepeatMode, newRepeatMode, trackPlayState);

            item.setRepeatMode(newRepeatMode);
            itemsChangedAndShouldRerender = itemsChangedAndShouldRerender || shouldRerenderItem;
        }
        return itemsChangedAndShouldRerender;
    }

    public void updateNowPlaying(int position, boolean notifyListener, boolean isPlaying) {

        if (items.size() == position && getItem(position).isTrack()
                && getItem(position).isPlayingOrPaused()) {
            return;
        }

        Optional<HeaderPlayQueueUIItem> lastHeaderPlayQueueUiItem = Optional.absent();
        boolean headerPlayStateSet = false;
        for (int i = 0; i < items.size(); i++) {
            final PlayQueueUIItem item = items.get(i);
            if (item.isTrack()) {
                final TrackPlayQueueUIItem trackItem = (TrackPlayQueueUIItem) item;
                setPlayState(position, i, trackItem, notifyListener, isPlaying);
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
        notifyItemInserted(position);
    }

    private void setPlayState(int currentlyPlayingPosition, int itemPosition, TrackPlayQueueUIItem item, boolean notifyListener, boolean isPlaying) {
        if (currentlyPlayingPosition == itemPosition) {
            item.setPlayState(isPlaying ? PlayState.PLAYING : PlayState.PAUSED);
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
        return trackItem.isPlayingOrPaused() || PlayState.COMING_UP.equals(trackItem.getPlayState());
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
        return input -> input.isTrack() &&
                ((TrackPlayQueueUIItem) input).getPlayQueueItem().equals(currentPlayQueueItem);
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
        trackRenderer.setTrackClickListener(trackClickListener);
    }

}
