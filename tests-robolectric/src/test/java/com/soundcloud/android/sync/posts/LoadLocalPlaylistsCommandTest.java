package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class LoadLocalPlaylistsCommandTest extends StorageIntegrationTest {

    private LoadLocalPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLocalPlaylistsCommand(propeller());
    }

    @Test
    public void loadsLocalPlaylists() throws Exception {
        ApiPlaylist playlist = testFixtures().insertLocalPlaylist();
        final ApiPlaylist privatePlaylist = ModelFixtures.create(ApiPlaylist.class);
        privatePlaylist.setSharing(Sharing.PRIVATE);
        testFixtures().insertLocalPlaylist(privatePlaylist);
        testFixtures().insertPlaylist();
        testFixtures().insertTrack();

        expect(command.call()).toContainExactlyInAnyOrder(
                expectedPropertySetFor(playlist),
                expectedPropertySetFor(privatePlaylist)
        );

    }

    private PropertySet expectedPropertySetFor(ApiPlaylist playlist) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.getUrn()),
                PlaylistProperty.TITLE.bind(playlist.getTitle()),
                PlaylistProperty.IS_PRIVATE.bind(!playlist.isPublic())
        );
    }
}