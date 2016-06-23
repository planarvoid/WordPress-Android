package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Collections;

public class PreviewCollectionItemTest extends AndroidUnitTest {

    @Test
    public void updatingCollectionPreviewPropertiesUpdatesLikesItem() {
        final LikesItem likesItem = LikesItem.fromTrackPreviews(singletonList(
                LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url")));
        final PreviewCollectionItem item = PreviewCollectionItem.forLikesAndStations(likesItem,
                                                                                     Collections.<StationRecord>emptyList());
        final PropertySet updateProperties = PropertySet.from(
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.REQUESTED));

        item.update(updateProperties);

        assertThat(likesItem.getDownloadState()).isEqualTo(OfflineState.REQUESTED);
    }

}
