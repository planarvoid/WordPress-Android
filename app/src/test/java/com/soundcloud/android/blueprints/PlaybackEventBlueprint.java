package com.soundcloud.android.blueprints;

import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(PlaybackSessionEvent.class)
public class PlaybackEventBlueprint {

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return PlaybackSessionEvent.forPlay(new Track(), 1L, new TrackSourceInfo("screen", true));
        }
    };

}
