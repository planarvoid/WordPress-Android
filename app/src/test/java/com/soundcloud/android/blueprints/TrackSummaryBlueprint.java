package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.TrackStats;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(TrackSummary.class)
public class TrackSummaryBlueprint {


    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new TrackSummary("soundcloud:sounds:4");
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

}
