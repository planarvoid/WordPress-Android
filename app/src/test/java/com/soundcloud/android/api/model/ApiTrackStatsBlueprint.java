package com.soundcloud.android.api.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(ApiTrackStats.class)
public class ApiTrackStatsBlueprint {

    @Default(force = true)
    int playbackCount = 789;
    @Default(force = true)
    int repostsCount = 12;
    @Default(force = true)
    int likesCount = 34;
    @Default(force = true)
    int commentsCount = 56;
}
