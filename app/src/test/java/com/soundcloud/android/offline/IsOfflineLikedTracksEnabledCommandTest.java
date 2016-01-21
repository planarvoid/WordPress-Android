package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class IsOfflineLikedTracksEnabledCommandTest extends StorageIntegrationTest {
    private IsOfflineLikedTracksEnabledCommand command;

    @Before
    public void setUp() throws Exception {
        command = new IsOfflineLikedTracksEnabledCommand(propeller());
    }

    @Test
    public void isOfflineLikesEnabledReturnsStoredValue() {
        testFixtures().insertLikesMarkedForOfflineSync();

        assertThat(command.call(null)).isTrue();
    }

    @Test
    public void isOfflineLikesEnabledReturnsFalseWhenNothingStoredInDB() {
        assertThat(command.call(null)).isFalse();
    }
}
