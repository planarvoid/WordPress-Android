package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TieredTracks;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayQueueUIItemMapper implements Func1<List<TrackItem>, List<PlayQueueUIItem>> {

    private final List<Long> numericIds = new ArrayList<>();

    @Override
    public List<PlayQueueUIItem> call(List<TrackItem> trackItems) {
        List<PlayQueueUIItem> playQueueUIItems = new ArrayList<>(trackItems.size());
        for (TrackItem trackItem : trackItems) {
            long numericId = trackItem.getUrn().getNumericId();
            long id = createUniqueId(numericId);
            numericIds.add(numericId);
            Urn urn = trackItem.getUrn();
            String title = trackItem.getTitle();
            String creator = trackItem.getCreatorName();
            boolean blocked = trackItem.isBlocked();
            int statusLabelId = getStatusLabelId(trackItem);
            ImageResource imageResource = getImageResource(trackItem);
            playQueueUIItems.add(new PlayQueueUIItem(id,
                                                     urn,
                                                     title,
                                                     creator,
                                                     blocked,
                                                     statusLabelId,
                                                     imageResource,
                                                     trackItem,
                                                     false));
        }

        return playQueueUIItems;
    }

    private ImageResource getImageResource(TrackItem trackItem) {
        Urn urn = trackItem.getUrn();
        PropertySet propertySet = trackItem.getSource();
        Optional<String> templateUrl = propertySet.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE,
                                                             Optional.<String>absent());
        return SimpleImageResource.create(urn, templateUrl);
    }

    private Long createUniqueId(long numericId) {
        final int occurrences = Collections.frequency(numericIds, numericId);
        if (occurrences == 0) {
            return numericId;
        }
        // Track ids are not supposed to be negative.
        // This avoids accidental collisions.
        return -1 * occurrences * numericId;
    }

    private int getStatusLabelId(TrackItem trackItem) {
        if (trackItem.isBlocked()) {
            return R.layout.not_available;
        } else if (TieredTracks.isHighTierPreview(trackItem)) {
            return R.layout.preview;
        } else if (TieredTracks.isFullHighTierTrack(trackItem)) {
            return R.layout.go_label;
        } else if (trackItem.isPrivate()) {
            return R.layout.private_label;
        } else {
            return Consts.NOT_SET;
        }
    }
}
