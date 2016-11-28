package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func2;

import android.content.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PlayQueueUIItemMapper implements Func2<List<TrackAndPlayQueueItem>, Map<Urn, String>, List<PlayQueueUIItem>> {

    private final Context context;
    private final PlayQueueManager playQueueManager;

    @Inject
    PlayQueueUIItemMapper(Context context, PlayQueueManager playQueueManager) {
        this.context = context;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public List<PlayQueueUIItem> call(List<TrackAndPlayQueueItem> items, Map<Urn, String> urnStringMap) {
        return new Mapper(urnStringMap).map(items);
    }

    private class Mapper {
        private final List<PlayQueueUIItem> uiItems = new ArrayList<>();
        private final PlayQueueManager.RepeatMode repeatMode;
        private final PlayQueueItem currentPlayQueueItem;
        private final Map<Urn, String> urnStringMap;
        private final boolean isShuffled;
        private final boolean isAutoPlay;
        private final boolean isStation;

        boolean pastCurrent = false;
        Optional<PlaybackContext> lastContext = Optional.absent();

        Mapper(Map<Urn, String> urnStringMap) {
            this.urnStringMap = urnStringMap;
            this.repeatMode = playQueueManager.getRepeatMode();
            this.isShuffled = playQueueManager.isShuffled();
            this.isAutoPlay = playQueueManager.isAutoPlay();
            this.isStation = playQueueManager.getCollectionUrn().isStation();
            this.currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        }

        public List<PlayQueueUIItem> map(List<TrackAndPlayQueueItem> items) {
            for (TrackAndPlayQueueItem item : items) {
                final TrackQueueItem playQueueItem = item.playQueueItem;

                if (isVisible(playQueueItem)) {
                    addHeaderIfNecessary(playQueueItem);
                    addTrack(item);
                }
                setPastCurrent(playQueueItem);
            }

            addMagicBox();

            return uiItems;
        }

        private void addMagicBox() {
            if (!isStation && !uiItems.isEmpty()) {
                uiItems.add(new MagicBoxPlayQueueUIItem(PlayState.COMING_UP, repeatMode, isAutoPlay));
            }
        }

        private void addHeaderIfNecessary(PlayableQueueItem playQueueItem) {
            final PlaybackContext playbackContext = playQueueItem.getPlaybackContext();
            final boolean canAddHeader = !pastCurrent || !isShuffled;

            if (canAddHeader && shouldAddNewHeader(playbackContext)) {
                lastContext = Optional.of(playbackContext);
                uiItems.add(new HeaderPlayQueueUIItem(playbackContext, getTitle(playQueueItem),
                                                      PlayState.COMING_UP, repeatMode));
            }
        }

        private void addTrack(TrackAndPlayQueueItem item) {
            uiItems.add(TrackPlayQueueUIItem.from(item.playQueueItem, item.trackItem, context,
                                                  getTitle(item.playQueueItem), repeatMode));
        }

        private void setPastCurrent(PlayQueueItem playQueueItem) {
            if (playQueueItem.equals(currentPlayQueueItem)) {
                pastCurrent = true;
            }
        }

        private boolean shouldAddNewHeader(PlaybackContext playbackContext) {
            return !lastContext.isPresent() || !playbackContext.equals(lastContext.get());
        }

        private Optional<String> getTitle(PlayableQueueItem item) {
            final Optional<Urn> urn = item.getPlaybackContext().urn();
            return urn.isPresent() ? Optional.fromNullable(urnStringMap.get(urn.get())) : Optional.<String>absent();
        }

        private boolean isVisible(PlayableQueueItem item) {
            return item.equals(currentPlayQueueItem) || item.isVisible();
        }
    }

}
