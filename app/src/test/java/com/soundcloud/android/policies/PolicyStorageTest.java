package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class PolicyStorageTest extends StorageIntegrationTest {

    private PolicyStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PolicyStorage(propellerRx());
    }

    @Test
    public void loadsBlockedStatiForStoredTracks() {
        final ApiTrack blockedTrack = testFixtures().insertBlockedTrack();
        final ApiTrack unblockedTrack = testFixtures().insertTrack();

        Map<Urn, Boolean> blockedStati = storage.loadBlockedStati(
                Arrays.asList(blockedTrack.getUrn(), unblockedTrack.getUrn())
        ).toBlocking().single();

        assertThat(blockedStati.get(blockedTrack.getUrn())).isTrue();
        assertThat(blockedStati.get(unblockedTrack.getUrn())).isFalse();
    }

    @Test
    public void loadsBlockedStatiLeavesOutUnstoredTrack() {
        final ApiTrack blockedTrack = testFixtures().insertBlockedTrack();
        final ApiTrack uninsertedTrack = ModelFixtures.create(ApiTrack.class);

        Map<Urn, Boolean> blockedStati = storage.loadBlockedStati(
                Arrays.asList(blockedTrack.getUrn(), uninsertedTrack.getUrn())
        ).toBlocking().single();

        assertThat(blockedStati.get(blockedTrack.getUrn())).isTrue();
        assertThat(blockedStati.containsKey(uninsertedTrack.getUrn())).isFalse();
    }
}
