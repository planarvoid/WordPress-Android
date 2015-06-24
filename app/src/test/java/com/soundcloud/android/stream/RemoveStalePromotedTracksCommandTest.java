package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Query.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.test.matchers.QueryMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class RemoveStalePromotedTracksCommandTest extends StorageIntegrationTest {

    private RemoveStalePromotedTracksCommand command;

    @Mock private DateProvider dateProvider;

    private final long now = System.currentTimeMillis();

    @Before
    public void setUp() throws Exception {
        command = new RemoveStalePromotedTracksCommand(propeller(), dateProvider);
    }

    @Test
    public void removesStalePromotedTrackAndReportsChange() throws Exception {
        testFixtures().insertPromotedTrackMetadata(123, now);
        when(dateProvider.getCurrentTime()).thenReturn(now + RemoveStalePromotedTracksCommand.STALE_TIME_MILLIS + 1);

        assertThat(command.call(null)).containsExactly(123L);
        expectPromotedTrackItemCountToBe(0);
    }

    @Test
    public void removesStalePromotedTrackFromStreamAndReportsChange() throws Exception {
        testFixtures().insertPromotedStreamTrack(testFixtures().insertTrack(), now, 123L);
        when(dateProvider.getCurrentTime()).thenReturn(now + RemoveStalePromotedTracksCommand.STALE_TIME_MILLIS + 1);

        assertThat(command.call(null)).containsExactly(123L);
        expectStreamItemCountToBe(0);
    }

    @Test
    public void doesNotRemovesNotStalePromotedTrackAndReportsNoChange() throws Exception {
        testFixtures().insertPromotedTrackMetadata(123, now);
        when(dateProvider.getCurrentTime()).thenReturn(now + RemoveStalePromotedTracksCommand.STALE_TIME_MILLIS);

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