package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


@RunWith(DefaultTestRunner.class)
public class TrackStorageTest {
    private TrackStorage storage;
    private ModelFactory modelFactory;

    @Before
    public void before() {
        storage = new TrackStorage();
        modelFactory = TestHelper.getModelFactory();
    }

    @Test
    public void shouldMarkTrackAsPlayed() throws Exception {
        Track track = modelFactory.createModel(Track.class);
        TestHelper.insertWithDependencies(track);

        final int PLAYS = 3;
        for (int i = 0; i < PLAYS; i++)
            expect(storage.markTrackAsPlayed(track)).toBeTrue();

        List<Track> allTracks = TestHelper.loadLocalContent(Content.TRACKS.uri, Track.class);
        expect(allTracks).toNumber(1);

        Track played = storage.getTrack(1L);
        expect(played.local_user_playback_count).toEqual(PLAYS);
    }
}
