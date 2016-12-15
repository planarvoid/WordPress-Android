package com.soundcloud.android.events;

import static com.soundcloud.android.events.EntityStateChangedEvent.forUpdate;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class EntityStateChangedEventTest extends AndroidUnitTest {

    @Test
    public void shouldIndicateSingularChangeEvents() {
        EntityStateChangedEvent multiChangeEvent = forUpdate(Arrays.asList(TestPropertySets.fromApiTrack(),
                                                                           TestPropertySets.fromApiTrack()));
        assertThat(multiChangeEvent.isSingularChange()).isFalse();

        EntityStateChangedEvent singleChangeEvent = forUpdate(TestPropertySets.fromApiTrack());
        assertThat(singleChangeEvent.isSingularChange()).isTrue();
    }

    @Test
    public void shouldReturnSingleUrnFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.forUpdate(Collections.singletonList(track));
        assertThat(singleChangeEvent.getFirstUrn()).isEqualTo(track.get(TrackProperty.URN));
    }

    @Test
    public void shouldReturnSingleChangeSetFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.forUpdate(Collections.singletonList(track));
        assertThat(singleChangeEvent.getNextChangeSet()).isEqualTo(track);
    }
}
