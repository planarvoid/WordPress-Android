package com.soundcloud.android.collection;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class CollectionItemTest extends AndroidUnitTest {
    @Test
    public void implementsEqualsContract() throws Exception {
        EqualsVerifier.forClass(CollectionItem.class)
                .withPrefabValues(PlaylistItem.class,
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)),
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)))
                .verify();
    }
}
