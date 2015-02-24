package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.Recording;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(Recording.class)
public class RecordingBlueprint {
    @Default(force = true) long duration = 86 * 1000;
    @Default(force = true) long userId = 123L;
    @Default(force = true) boolean isPrivate = true;
    @Default String title = "Pending recording title";
}
