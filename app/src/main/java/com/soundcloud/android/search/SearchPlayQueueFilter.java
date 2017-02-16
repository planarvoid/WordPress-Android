package com.soundcloud.android.search;

import com.soundcloud.android.configuration.experiments.SearchPlayRelatedTracksConfig;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class SearchPlayQueueFilter {
    private final SearchPlayRelatedTracksConfig playRelatedTracksConfig;

    @Inject
    SearchPlayQueueFilter(SearchPlayRelatedTracksConfig playRelatedTracksConfig) {
        this.playRelatedTracksConfig = playRelatedTracksConfig;
    }

    public <T> List<T> correctQueue(List<T> playQueue, int position) {
        if (playRelatedTracksConfig.isEnabled()) {
            return Collections.singletonList(playQueue.get(position));
        } else {
            return playQueue;
        }
    }

    public int correctPosition(int position) {
        return playRelatedTracksConfig.isEnabled() ? 0 : position;
    }
}
