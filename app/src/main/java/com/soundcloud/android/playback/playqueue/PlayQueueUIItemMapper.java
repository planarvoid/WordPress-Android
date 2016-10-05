package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayableQueueItem;
import com.soundcloud.android.playback.PlaybackContext;
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
        final List<PlayQueueUIItem> uiItems = new ArrayList<>();
        Optional<PlaybackContext> lastContext = Optional.absent();

        for (TrackAndPlayQueueItem item : items) {
            final PlayQueueItem playQueueItem = item.playQueueItem;
            final PlaybackContext playbackContext = ((PlayableQueueItem) playQueueItem).getPlaybackContext();
            final Optional<String> title = getTitle(urnStringMap,playbackContext);

            if (!playQueueManager.isShuffled() && shouldAddNewHeader(lastContext, playbackContext)) {
                lastContext = Optional.of(playbackContext);
                uiItems.add(new HeaderPlayQueueUIItem(playbackContext, title));
            }
            uiItems.add(TrackPlayQueueUIItem.from(playQueueItem, item.trackItem, context, title, playQueueManager.getRepeatMode()));
        }
        return uiItems;
    }

    private static boolean shouldAddNewHeader(Optional<PlaybackContext> lastContext,
                                              PlaybackContext playbackContext) {
        return !lastContext.isPresent() || !playbackContext.equals(lastContext.get());
    }

    private static Optional<String> getTitle(Map<Urn, String> urnTitles, PlaybackContext playbackContext) {
        final Optional<Urn> urn = playbackContext.urn();
        return urn.isPresent() ?
               Optional.fromNullable(urnTitles.get(urn.get())) :
               Optional.<String>absent();
    }

}
