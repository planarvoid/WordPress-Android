package com.soundcloud.android.blueprints;

import com.soundcloud.android.ads.VisualAd;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

@Blueprint(VisualAd.class)
public class VisualAdBlueprint {

    @Default
    String imageUrl = "http://image.visualad.com";

    @Default
    String clickthroughUrl = "http://clickthrough.visualad.com";

}
