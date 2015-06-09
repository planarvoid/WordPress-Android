package com.soundcloud.android.api.legacy.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Deprecated
@Blueprint(PublicApiTrack.class)
public class PublicApiTrackBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new PublicApiTrack(runningId++);
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    PublicApiUser user;

    @Default
    Date createdAt = new Date();

    @Default
    String policy = "monetizable";

    @Default
    String genre = "genre";

    @Default
    String permalinkUrl = "permalink-url";

    @Default
    String streamUrl = "stream-url";

    @Default
    String waveformUrl = "waveform-url";
}
