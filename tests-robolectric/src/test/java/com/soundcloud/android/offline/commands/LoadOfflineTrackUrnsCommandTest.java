package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
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
public class LoadOfflineTrackUrnsCommandTest extends StorageIntegrationTest {

    private LoadOfflineTrackUrnsCommand command;

    @Before
    public void setup() {
        command = new LoadOfflineTrackUrnsCommand(propeller());
    }

    @Test
    public void shouldLoadTrackLikes() throws Exception {
        testFixtures().insertTrackDownloadPendingRemoval(Urn.forTrack(234L), new Date(200).getTime());
        final Urn downloadUrn = insertOfflineLike(100);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(downloadUrn);
    }

    @Test
    public void shouldLoadTrackLikesOrderedByLikeDate() throws Exception {
        final Urn track1 = insertOfflineLike(100);
        final Urn track2 = insertOfflineLike(200);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(track2, track1);
    }

    private Urn insertOfflineLike(long likedAt) {
        final PropertySet track1 = testFixtures().insertLikedTrack(new Date(likedAt)).toPropertySet();
        final Urn trackUrn = track1.get(TrackProperty.URN);
        testFixtures().insertCompletedTrackDownload(trackUrn, 100, 200);

        return trackUrn;
    }
}