package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.query.WhereBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadTracksWithStalePoliciesCommandTest extends StorageIntegrationTest {

    private LoadTracksWithStalePoliciesCommand command;

    @Before
    public void setup() {
        command = new LoadTracksWithStalePoliciesCommand(propeller());
    }

    @Test
    public void loadsLikeWithStalePolicy() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicyTimestamp(apiTrack, new Date(200));

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(apiTrack.getUrn());
    }

    @Test
    public void loadsLikeWithMissingPolicy() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        database().delete(Table.TrackPolicies.name(),
                new WhereBuilder().whereEq(TableColumns.TrackPolicies.TRACK_ID, apiTrack.getId()).build(), null);

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(apiTrack.getUrn());
    }

    @Test
    public void ignoresLikeWithUpToDatePolicy() throws Exception {
        ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date(100));
        updatePolicyTimestamp(apiTrack, new Date());

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toBeEmpty();
    }

    private void updatePolicyTimestamp(ApiTrack track, Date date) {
        database().execSQL("UPDATE TrackPolicies SET last_updated = " + date.getTime() + " where track_id=" + track.getId());
    }

}
