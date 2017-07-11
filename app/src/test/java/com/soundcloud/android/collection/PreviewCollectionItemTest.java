package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PreviewCollectionItemTest {

    @Test
    public void updatingCollectionPreviewPropertiesUpdatesLikesItem() {
        final LikesItem likesItem = LikesItem.fromTrackPreviews(singletonList(
                LikedTrackPreview.create(Urn.forTrack(123L), "http://image-url")));
        final PreviewCollectionItem item = PreviewCollectionItem.forLikesPlaylistsAndStations(
                likesItem, Optional.of(Collections.emptyList()), Optional.of(Collections.emptyList()), Optional.of(Collections.emptyList()), Collections.emptyList());

        final PreviewCollectionItem updatedItem = item.updatedWithOfflineState(OfflineState.REQUESTED);

        assertThat(updatedItem.getLikes().offlineState()).isEqualTo(OfflineState.REQUESTED);
    }

}
