package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;

@RunWith(SoundCloudTestRunner.class)
public class TrackingInfoTest {

    @Test
    public void shouldBeEqualWithJustSourceContext() throws Exception {
        final TrackingInfo actual = new TrackingInfo("source-context");
        final TrackingInfo expected = new TrackingInfo("source-context");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldBeEqualWithSourceAndExplore() throws Exception {
        final TrackingInfo actual = new TrackingInfo("source-context", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("source-context", "explore-tag");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentExplore() throws Exception {
        final TrackingInfo actual = new TrackingInfo("source-context", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("source-context", "explore-tag2");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentSource() throws Exception {
        final TrackingInfo actual = new TrackingInfo("source-context", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("source-context2", "explore-tag");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldBeSerializableFromJustExploreTag() throws Exception {
        Serializable original = new TrackingInfo("source-context");
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);

    }
}
