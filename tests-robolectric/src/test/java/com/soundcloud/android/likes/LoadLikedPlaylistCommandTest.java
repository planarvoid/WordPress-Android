package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikedPlaylistCommandTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);

    private LoadLikedPlaylistCommand command;
    private PropertySet playlist;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedPlaylistCommand(propeller());
    }

    @Test
    public void emitsEmptyPropertySetIfLikeDoesNotExist() throws Exception {
        playlist = testFixtures().insertPlaylist().toPropertySet();

        PropertySet result = command.with(playlist.get(TrackProperty.URN)).call();

        expect(result).toEqual(PropertySet.create());
    }

    @Test
    public void loadsTrackLike() throws Exception {
        playlist = testFixtures().insertLikedPlaylist(LIKED_DATE_1).toPropertySet();

        PropertySet result = command.with(playlist.get(TrackProperty.URN)).call();

        expect(result).toEqual(LoadLikedPlaylistsCommandTest.expectedLikedPlaylistFor(playlist, LIKED_DATE_1));
    }
}