package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LoadPolicyUpdateTimeCommandTest extends StorageIntegrationTest {

    private LoadPolicyUpdateTimeCommand command;

    @Before
    public void setUp() {
        command = new LoadPolicyUpdateTimeCommand(propeller());
    }

    @Test
    public void returnsNotSetWhenNoPoliciesInDB() {
        assertThat(command.call(null)).isEqualTo(Consts.NOT_SET);
    }

    @Test
    public void returnMostRecentUpdateDate() {
        final Date now = new Date();
        final Date yesterday = new Date(now.getTime() - TimeUnit.DAYS.toMillis(1));
        final Date daysAgo = new Date(now.getTime() - TimeUnit.DAYS.toMillis(3));

        testFixtures().insertPolicyAllow(Urn.forTrack(123L), yesterday.getTime());
        testFixtures().insertPolicyAllow(Urn.forTrack(124L), now.getTime());
        testFixtures().insertPolicyAllow(Urn.forTrack(125L), daysAgo.getTime());

        assertThat(command.call(null)).isEqualTo(now.getTime());
    }
}
