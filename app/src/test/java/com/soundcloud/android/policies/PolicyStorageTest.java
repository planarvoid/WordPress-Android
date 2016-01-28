package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class PolicyStorageTest extends StorageIntegrationTest {

    private PolicyStorage storage;

    @Test
    public void loadsblockedStatusesForStoredTracks() {
        storage = new PolicyStorage(propellerRx());

        final ApiTrack blockedTrack = testFixtures().insertBlockedTrack();
        final ApiTrack unblockedTrack = testFixtures().insertTrack();

        Map<Urn, Boolean> blockedStatuses = storage.loadBlockedStatuses(
                Arrays.asList(blockedTrack.getUrn(), unblockedTrack.getUrn())
        ).toBlocking().single();

        assertThat(blockedStatuses.get(blockedTrack.getUrn())).isTrue();
        assertThat(blockedStatuses.get(unblockedTrack.getUrn())).isFalse();
    }

    @Test
    public void loadsblockedStatusesForStoredTracksInBatches() {
        storage = new PolicyStorage(propellerRx(), 2);

        final ApiTrack blockedTrack = testFixtures().insertBlockedTrack();
        final ApiTrack blockedTrack2 = testFixtures().insertBlockedTrack();
        final ApiTrack unblockedTrack = testFixtures().insertTrack();

        Map<Urn, Boolean> blockedStatuses = storage.loadBlockedStatuses(
                Arrays.asList(blockedTrack.getUrn(), blockedTrack2.getUrn(), unblockedTrack.getUrn())
        ).toBlocking().single();

        assertThat(blockedStatuses.get(blockedTrack.getUrn())).isTrue();
        assertThat(blockedStatuses.get(blockedTrack2.getUrn())).isTrue();
        assertThat(blockedStatuses.get(unblockedTrack.getUrn())).isFalse();
    }

    @Test
    public void loadsblockedStatusesLeavesOutUnstoredTrack() {
        storage = new PolicyStorage(propellerRx());

        final ApiTrack blockedTrack = testFixtures().insertBlockedTrack();
        final ApiTrack uninsertedTrack = ModelFixtures.create(ApiTrack.class);

        Map<Urn, Boolean> blockedStatuses = storage.loadBlockedStatuses(
                Arrays.asList(blockedTrack.getUrn(), uninsertedTrack.getUrn())
        ).toBlocking().single();

        assertThat(blockedStatuses.get(blockedTrack.getUrn())).isTrue();
        assertThat(blockedStatuses.containsKey(uninsertedTrack.getUrn())).isFalse();
    }
}
