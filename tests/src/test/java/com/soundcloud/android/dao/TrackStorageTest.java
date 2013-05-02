package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


@RunWith(DefaultTestRunner.class)
public class TrackStorageTest {
    TrackStorage storage;

    @Before
    public void before() {
        storage = new TrackStorage();
    }

    @Test
    public void shouldMarkTrackAsPlayed() throws Exception {
        Track track = new Track();
        track.id = 100L;
        track.title = "testing";
        track.user = new User();
        track.user.id = 200L;

        TestHelper.insertWithDependencies(track);

        final int PLAYS = 3;
        for (int i = 0; i < PLAYS; i++)
            expect(storage.markTrackAsPlayed(track)).toBeTrue();

        List<Track> allTracks = TestHelper.loadLocalContent(Content.TRACKS.uri, Track.class);
        expect(allTracks).toNumber(1);

        Track played = storage.getTrack(100L);
        expect(played.local_user_playback_count).toEqual(PLAYS);
    }
}
