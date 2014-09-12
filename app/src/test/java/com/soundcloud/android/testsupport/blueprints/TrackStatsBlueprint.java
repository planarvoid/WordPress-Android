package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.TrackStats;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(TrackStats.class)
public class TrackStatsBlueprint {

    @Default(force = true)
    int playbackCount = 789;
    @Default(force = true)
    int repostsCount = 12;
    @Default(force = true)
    int likesCount = 34;
    @Default(force = true)
    int commentsCount = 56;
}
