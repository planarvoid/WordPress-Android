package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadPlaylistCommandTest extends StorageIntegrationTest {

    private LoadPlaylistCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistCommand(propeller());
    }

    @Test
    public void returnsEmptyPropertySetIfNotStored() throws Exception {
        PropertySet playlist = command.with(Urn.forPlaylist(123)).call();

        expect(playlist).toEqual(PropertySet.create());
    }

    @Test
    public void loadsPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();

        PropertySet playlist = command.with(apiPlaylist.getUrn()).call();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, false, false));
    }

    @Test
    public void loadsLikedPlaylistFromDatabase() throws Exception {
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date(100));

        PropertySet playlist = command.with(apiPlaylist.getUrn()).call();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, true, false));
    }

    @Test
    public void loadsRepostedPlaylistFromDatabase() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepostCollectionItem(apiPlaylist.getId());

        PropertySet playlist = command.with(apiPlaylist.getUrn()).call();

        expect(playlist).toEqual(TestPropertySets.fromApiPlaylist(apiPlaylist, false, true));
    }
}