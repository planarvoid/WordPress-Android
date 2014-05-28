package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.TrackStats;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(TrackStats.class)
public class TrackStatsBlueprint {

    @Default(force = true)
    long playbackCount = 789L;
    @Default(force = true)
    long repostsCount = 12L;
    @Default(force = true)
    long likesCount = 34L;
    @Default(force = true)
    long commentsCount = 56L;
}
