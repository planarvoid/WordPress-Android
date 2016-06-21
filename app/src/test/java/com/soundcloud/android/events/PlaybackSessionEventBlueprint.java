package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(PlaybackSessionEvent.class)
public class PlaybackSessionEventBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return PlaybackSessionEvent.forPlay(
                    PlaybackSessionEventArgs.create(
                            TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L), Urn.forUser(2L)), Urn.forUser(1L),
                            new TrackSourceInfo("screen", true), 456L, "hls", "playa", "3g", false, false, ""
                    )
            );
        }
    };

}
