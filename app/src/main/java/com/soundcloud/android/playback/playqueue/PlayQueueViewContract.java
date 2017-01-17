package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;

import java.util.List;

public interface PlayQueueViewContract {

    void setShuffledState(boolean shuffled);

    void removeLoadingIndicator();

    void setRepeatMode(PlayQueueManager.RepeatMode nextRepeatMode);

    void scrollTo(int position);

    int getAdapterPosition(PlayQueueItem currentPlayQueueItem);

    int getItemCount();

    PlayQueueUIItem getItem(int adapterPosition);

    void removeItem(int adapterPosition);

    void switchItems(int fromPosition, int toPosition);

    int getQueuePosition(int fromAdapterPosition);

    List<PlayQueueUIItem> getItems();

    void clear();

    void addItem(int position, PlayQueueUIItem item);

    void addItem(PlayQueueUIItem item);

    void notifyDataSetChanged();

    void updateNowPlaying(int adapterPosition, boolean notifyListener, boolean isPlaying);

    boolean isEmpty();

    void showUndo();
}
