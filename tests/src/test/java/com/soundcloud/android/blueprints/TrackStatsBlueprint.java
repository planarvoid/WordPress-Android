package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.TrackStats;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(TrackStats.class)
public class TrackStatsBlueprint {

    @Default
    long playbackCount = 789L;
    @Default
    long repostsCount = 12L;
    @Default
    long likesCount = 34L;
    @Default
    long commentsCount = 56L;
}
