package com.soundcloud.android.collections;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class CollectionsItemTest extends AndroidUnitTest {
    @Test
    public void implementsEqualsContract() throws Exception {
        EqualsVerifier.forClass(CollectionsItem.class)
                .withPrefabValues(PlaylistItem.class,
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)),
                        PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)))
                .verify();
    }
}
