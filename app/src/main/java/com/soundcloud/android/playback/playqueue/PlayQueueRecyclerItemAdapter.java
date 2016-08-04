package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.TracksRecyclerItemAdapter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PlayQueueRecyclerItemAdapter extends TracksRecyclerItemAdapter {
    private final List<Long> numericID = new ArrayList<>();
    private final List<Long> uniqueID = new ArrayList<>();

    @Inject
    public PlayQueueRecyclerItemAdapter(PlayQueueItemRenderer playQueueItemRenderer) {
        super(playQueueItemRenderer);
        setHasStableIds(true);
    }

    @Override
    public void clear() {
        numericID.clear();
        uniqueID.clear();
        super.clear();
    }

    @Override
    public void addItem(TrackItem item) {
        final long numericId = item.getUrn().getNumericId();
        uniqueID.add(createUniqueId(numericId));
        numericID.add(numericId);
        super.addItem(item);
    }

    private Long createUniqueId(long numericId) {
        final int occurrences = Collections.frequency(numericID, numericId);
        if (occurrences == 0) {
            return numericId;
        }
        // Track ids are not supposed to be negative.
        // This avoids accidental collisions.
        return -1 * occurrences * numericId;
    }

    @Override
    public long getItemId(int position) {
        return uniqueID.get(position);
    }

}
