package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Query.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.test.matchers.QueryMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class RemoveStalePromotedItemsCommandTest extends StorageIntegrationTest {

    private RemoveStalePromotedItemsCommand command;


    private final long now = System.currentTimeMillis();
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider(now);
        command = new RemoveStalePromotedItemsCommand(propeller(), dateProvider);
    }

    @Test
    public void removesStalePromotedTrackAndReportsChange() throws Exception {
        testFixtures().insertPromotedTrackMetadata(123, now);
        dateProvider.advanceBy(RemoveStalePromotedItemsCommand.STALE_TIME_MILLIS + 1, TimeUnit.MILLISECONDS);

        assertThat(command.call(null)).containsExactly(123L);
        expectPromotedTrackItemCountToBe(0);
    }

    @Test
    public void removesStalePromotedTrackFromStreamAndReportsChange() throws Exception {
        testFixtures().insertPromotedStreamTrack(testFixtures().insertTrack(), now, 123L);
        dateProvider.advanceBy(RemoveStalePromotedItemsCommand.STALE_TIME_MILLIS + 1, TimeUnit.MILLISECONDS);

        assertThat(command.call(null)).containsExactly(123L);
        expectStreamItemCountToBe(0);
    }

    @Test
    public void doesNotRemovesNotStalePromotedTrackAndReportsNoChange() throws Exception {
        testFixtures().insertPromotedTrackMetadata(123, now);
        dateProvider.advanceBy(RemoveStalePromotedItemsCommand.STALE_TIME_MILLIS, TimeUnit.MILLISECONDS);

        assertThat(command.call(null)).isEmpty();
        expectPromotedTrackItemCountToBe(1);
    }

    private void expectPromotedTrackItemCountToBe(int count) {
        assertThat(select(from(Table.PromotedTracks.name())), QueryMatchers.counts(count));
    }

    private void expectStreamItemCountToBe(int count) {
        assertThat(select(from(Table.SoundStream.name())), QueryMatchers.counts(count));
    }
}
