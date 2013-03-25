package com.soundcloud.android.dao;

import android.net.Uri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;


@RunWith(DefaultTestRunner.class)
public class TrackStorageTest {
    TrackStorage storage;

    @Before
    public void before() {
        storage = new TrackStorage(Robolectric.application);
    }

    @Test
    public void shouldMarkTrackAsPlayed() throws Exception {
        Track track = new Track();
        track.id = 100L;
        track.title = "testing";
        track.user = new User();
        track.user.id = 200L;
        Uri uri = storage.create(track);
        expect(uri).not.toBeNull();

        final int PLAYS = 3;
        for (int i = 0; i < PLAYS; i++)
            expect(storage.markTrackAsPlayed(track)).toBeTrue();

        //List<Track> allTracks = storage.allTracks();
        //expect(allTracks).toNumber(1);

        Track played = storage.getTrack(100L);
        expect(played.local_user_playback_count).toEqual(PLAYS);
    }
}
