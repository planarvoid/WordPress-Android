package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTracksOperationsTest {

    private SuggestedTracksOperations suggestedTracksOperations;

    @Before
    public void setUp(){
        suggestedTracksOperations = new SuggestedTracksOperations();
    }

    @Test
    public void shouldReturnDummySuggestedTracks(){
        List<SuggestedTrack> trackList = Lists.newArrayList(suggestedTracksOperations.getPopMusic().toBlockingObservable().toIterable());
        expect(trackList.size()).toBe(8);
        expect(trackList.get(0).getTitle()).toEqual("Evolution of Get Lucky [Daft Punk chronologic cover]");
        expect(trackList.get(0).getStreamUrl()).toEqual("https://api.soundcloud.com/tracks/96017719/stream");
    }
}
