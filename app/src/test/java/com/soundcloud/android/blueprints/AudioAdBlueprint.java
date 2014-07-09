package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.ads.AudioAd;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Mapped;

@Blueprint(AudioAd.class)
public class AudioAdBlueprint {

    @Mapped
    TrackSummary trackSummary;

}
