package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class EntityStateChangedEventTest {

    @Test
    public void shouldIndicateSingularChangeEvents() {
        EntityStateChangedEvent multiChangeEvent = EntityStateChangedEvent.fromSync(
                Arrays.asList(TestPropertySets.fromApiTrack(), TestPropertySets.fromApiTrack()));
        expect(multiChangeEvent.isSingularChange()).toBeFalse();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(TestPropertySets.fromApiTrack());
        expect(singleChangeEvent.isSingularChange()).toBeTrue();
    }

    @Test
    public void shouldReturnSingleUrnFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(track);
        expect(singleChangeEvent.getNextUrn()).toEqual(track.get(TrackProperty.URN));
    }

    @Test
    public void shouldReturnSingleChangeSetFromSingularChangeEvent() {
        PropertySet track = TestPropertySets.fromApiTrack();
        EntityStateChangedEvent singleChangeEvent = EntityStateChangedEvent.fromSync(track);
        expect(singleChangeEvent.getNextChangeSet()).toEqual(track);
    }
}