package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import java.util.Collections;

public class CollectionItemTest extends AndroidUnitTest {

    @Test
    public void implementsEqualsContract() {
        EqualsVerifier.forClass(CollectionItem.class)
                .withPrefabValues(PlaylistItem.class,
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)),
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)))
                .verify();
    }

    @Test
    public void updatingCollectionPreviewPropertiesUpdatesLikesItem() {
        final LikesItem likesItem = LikesItem.fromUrns(Collections.singletonList(Urn.forTrack(123L)));
        final CollectionItem item = CollectionItem.fromCollectionsPreview(likesItem, Collections.<Urn>emptyList());
        final PropertySet updateProperties = PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(OfflineState.REQUESTED));

        item.update(updateProperties);

        assertThat(likesItem.getDownloadState()).isEqualTo(OfflineState.REQUESTED);
    }

}
