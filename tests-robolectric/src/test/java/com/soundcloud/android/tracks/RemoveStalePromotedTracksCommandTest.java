package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
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

        expect(command.call(null)).toContainExactly(123L);
        expectPromotedTrackItemCountToBe(0);
    }

    @Test
    public void removesStalePromotedTrackFromStreamAndReportsChange() throws Exception {
        testFixtures().insertPromotedStreamTrack(testFixtures().insertTrack(), now, 123L);
        when(dateProvider.getCurrentTime()).thenReturn(now + RemoveStalePromotedTracksCommand.STALE_TIME_MILLIS + 1);

        expect(command.call(null)).toContainExactly(123L);
        expectStreamItemCountToBe(0);
    }

    @Test
    public void doesNotRemovesNotStalePromotedTrackAndReportsNoChange() throws Exception {
        testFixtures().insertPromotedTrackMetadata(123, now);
        when(dateProvider.getCurrentTime()).thenReturn(now + RemoveStalePromotedTracksCommand.STALE_TIME_MILLIS);

        expect(command.call(null)).toBeEmpty();
        expectPromotedTrackItemCountToBe(1);
    }

    private void expectPromotedTrackItemCountToBe(int count) {
        assertThat(select(from(Table.PromotedTracks.name())), counts(count));
    }

    private void expectStreamItemCountToBe(int count) {
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }
}