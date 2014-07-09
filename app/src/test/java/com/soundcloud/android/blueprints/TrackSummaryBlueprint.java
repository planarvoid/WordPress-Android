package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.TrackStats;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Date;

@Blueprint(TrackSummary.class)
public class TrackSummaryBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new TrackSummary(String.format("soundcloud:sounds:%d", runningId++));
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    UserSummary user;

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
