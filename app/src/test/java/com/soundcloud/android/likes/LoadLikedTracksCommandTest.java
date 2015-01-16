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
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadLikedTracksCommandTest extends StorageIntegrationTest {

    private LoadLikedTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTracksCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikesFromObservable() throws Exception {
        PropertySet track1 = testFixtures().insertLikedTrack(new Date(100)).toPropertySet();
        PropertySet track2 = testFixtures().insertLikedTrack(new Date(200)).toPropertySet();

        List<PropertySet> result = command.call();

        expect(result).toContainExactly(expectedLikedTrackFor(track2), expectedLikedTrackFor(track1));
    }

    private PropertySet expectedLikedTrackFor(PropertySet track) {
        return PropertySet.from(
                TrackProperty.URN.bind(track.get(TrackProperty.URN)),
                TrackProperty.CREATOR_NAME.bind(track.get(TrackProperty.CREATOR_NAME)),
                TrackProperty.TITLE.bind(track.get(TrackProperty.TITLE)),
                TrackProperty.DURATION.bind(track.get(TrackProperty.DURATION)),
                TrackProperty.PLAY_COUNT.bind(track.get(TrackProperty.PLAY_COUNT)),
                TrackProperty.LIKES_COUNT.bind(track.get(TrackProperty.LIKES_COUNT)),
                TrackProperty.IS_PRIVATE.bind(track.get(TrackProperty.IS_PRIVATE)));
    }
}