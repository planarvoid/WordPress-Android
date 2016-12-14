package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import java.util.Collections;

public class PreviewCollectionItemTest extends AndroidUnitTest {

    @Test
    public void updatingCollectionPreviewPropertiesUpdatesLikesItem() {
        final LikesItem likesItem = LikesItem.fromTrackPreviews(singletonList(
                LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url")));
        final PreviewCollectionItem item = PreviewCollectionItem.forLikesPlaylistsAndStations(
                likesItem, Collections.emptyList(), Collections.emptyList());

        final PreviewCollectionItem updatedItem = item.updatedWithOfflineState(OfflineState.REQUESTED);

        assertThat(updatedItem.getLikes().offlineState()).isEqualTo(OfflineState.REQUESTED);
    }

}
