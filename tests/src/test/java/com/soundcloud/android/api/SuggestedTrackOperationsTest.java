package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTrackOperationsTest {

    private SuggestedTrackOperations suggestedTrackOperations;

    @Before
    public void setUp(){
        suggestedTrackOperations = new SuggestedTrackOperations(Robolectric.application);
    }

    @Test
    public void shouldReturnDummySuggestedTracks(){
        List<SuggestedTrack> trackList = Lists.newArrayList(suggestedTrackOperations.getPopMusic().toBlockingObservable().toIterable());
        expect(trackList.size()).toBe(8);
        expect(trackList.get(0).getTitle()).toEqual("Let go 2");
        expect(trackList.get(0).getStreamUrl()).toEqual("https://api.soundcloud.com/tracks/62848613/stream");
    }
}
