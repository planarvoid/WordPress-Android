package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadTracksWithValidPoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithValidPoliciesCommand command;

    @Before
    public void setup() {
        command = new LoadTracksWithValidPoliciesCommand(propeller());
    }

    @Test
    public void loadsLikeWithValidPolicy() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicy(apiTrack, true);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(apiTrack.getUrn());
    }

    @Test
    public void loadLikesWithValidPolicyOrderedByLikedDate() throws Exception {
        ApiTrack apiTrack1 = testFixtures().insertLikedTrack(new Date(100));
        updatePolicy(apiTrack1, true);
        ApiTrack apiTrack2 = testFixtures().insertLikedTrack(new Date(200));
        updatePolicy(apiTrack2, true);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(apiTrack2.getUrn(), apiTrack1.getUrn());
    }

    @Test
    public void ignoresLikesThatShouldNotBeDownloaded() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicy(apiTrack, false);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toBeEmpty();
    }

    @Test
    public void ignoresLikesWithoutPolicy() throws Exception {
        testFixtures().insertLikedTrack(new Date(100));

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toBeEmpty();
    }

    private void updatePolicy(ApiTrack track, boolean syncable) {
        database().execSQL("UPDATE TrackPolicies SET syncable = " + (syncable ? 1 : 0) + " where track_id=" + track.getId());
    }

}
