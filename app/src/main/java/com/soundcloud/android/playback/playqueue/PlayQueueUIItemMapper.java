package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
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

    @Inject
    PlayQueueUIItemMapper(Context context) {
        this.context = context;
    }

    @Override
    public List<PlayQueueUIItem> call(List<TrackAndPlayQueueItem> trackAndPlayQueueItems,
                                      Map<Urn, String> urnStringMap) {
        final List<PlayQueueUIItem> items = new ArrayList<>();
        Optional<PlaybackContext> lastContext = Optional.absent();

        for (TrackAndPlayQueueItem item : trackAndPlayQueueItems) {
            final PlayQueueItem playQueueItem = item.playQueueItem;
            final PlaybackContext playbackContext = ((PlayableQueueItem) playQueueItem).getPlaybackContext();

            if (shouldAddNewHeader(lastContext, playbackContext)) {
                lastContext = Optional.of(playbackContext);
                items.add(new HeaderPlayQueueUIItem(playbackContext, getTitle(urnStringMap, playbackContext)));
            }
            items.add(TrackPlayQueueUIItem.from(playQueueItem, item.trackItem, context));
        }
        return items;
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
