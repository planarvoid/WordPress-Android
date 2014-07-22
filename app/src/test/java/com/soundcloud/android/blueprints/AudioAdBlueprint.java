package com.soundcloud.android.blueprints;

import com.soundcloud.android.ads.VisualAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.ads.AudioAd;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Mapped;

@Blueprint(AudioAd.class)
public class AudioAdBlueprint {

    @Mapped
    ApiTrack apiTrack;

    @Mapped
    VisualAd visualAd;
}
