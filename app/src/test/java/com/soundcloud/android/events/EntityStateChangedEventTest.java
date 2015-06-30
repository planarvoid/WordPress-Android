package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;

import java.util.Arrays;

public class EntityStateChangedEventTest extends PlatformUnitTest {

    @Test
    public void shouldIndicateSingularChangeEvents() {
        EntityStateChangedEvent multiChangeEvent = EntityStateChangedEvent.fromSync(
                Arrays.asList(TestPropertySets.fromApiTrack(), TestPropertySets.fromApiTrack()));
        assertThat(multiChangeEvent.isSingularChange()).isFalse();

        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(TestPropertySets.fromApiTrack());
        assertThat(singleChangeEvent.isSingularChange()).isTrue();
    }

    @Test
    public void shouldReturnSingleUrnFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(track);
        assertThat(singleChangeEvent.getFirstUrn()).isEqualTo(track.get(TrackProperty.URN));
    }

    @Test
    public void shouldReturnSingleChangeSetFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(track);
        assertThat(singleChangeEvent.getNextChangeSet()).isEqualTo(track);
    }
}