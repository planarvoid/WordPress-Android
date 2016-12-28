package com.soundcloud.android.events;

import static com.soundcloud.android.events.EntityStateChangedEvent.mergeUpdates;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;

import java.util.Arrays;

public class EntityStateChangedEventTest extends AndroidUnitTest {

    @Test
    public void shouldIndicateSingularChangeEvents() {
        EntityStateChangedEvent multiChangeEvent = mergeUpdates(Arrays.asList(TestPropertySets.fromApiTrack().toUpdateEvent(),
                                                                           TestPropertySets.fromApiTrack().toUpdateEvent()));
        assertThat(multiChangeEvent.isSingularChange()).isFalse();

        EntityStateChangedEvent singleChangeEvent = TestPropertySets.fromApiTrack().toUpdateEvent();
        assertThat(singleChangeEvent.isSingularChange()).isTrue();
    }

    @Test
    public void shouldReturnSingleUrnFromSingularChangeEvent() {
        TrackItem track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = track.toUpdateEvent();
        assertThat(singleChangeEvent.getFirstUrn()).isEqualTo(track.getUrn());
    }

    @Test
    public void shouldReturnSingleChangeSetFromSingularChangeEvent() {
        TrackItem track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = track.toUpdateEvent();
        assertThat(TrackItem.from(singleChangeEvent.getNextChangeSet())).isEqualTo(track);
    }
}
