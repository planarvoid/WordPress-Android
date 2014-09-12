package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(PlaybackSessionEvent.class)
public class PlaybackEventBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return PlaybackSessionEvent.forPlay(
                    TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L)),
                    Urn.forUser(1L), "hls", new TrackSourceInfo("screen", true), 456L, 12345L);
        }
    };

}
