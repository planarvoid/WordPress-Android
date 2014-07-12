package com.soundcloud.android.blueprints;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.legacy.model.TrackStats;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Date;

@Blueprint(ApiTrack.class)
public class TrackSummaryBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiTrack(String.format("soundcloud:sounds:%d", runningId++));
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    ApiUser user;

    @Mapped
    TrackStats stats;

    @Default
    String streamUrl = "http://media.soundcloud.com/stream/whVhoRw2gpUh";

    @Default(force = true)
    int duration = 12345;

    @Default
    String waveformUrl = "http://waveform.url";

    @Default
    String permalinkUrl = "https://soundcloud.com/bismakarisma/kuatkitabersinar";

    @Default
    Sharing sharing = Sharing.PUBLIC;

    @Default
    Date createdAt = new Date();

    @Default
    String genre = "Clownstep";
}
