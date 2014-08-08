package com.soundcloud.android.blueprints;

import com.soundcloud.android.ads.VisualAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(AudioAd.class)
public class AudioAdBlueprint {

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            ModelFactory factory = TestHelper.getModelFactory();
            try {
                return new AudioAd(
                        factory.createModel(ApiTrack.class),
                        factory.createModel(VisualAd.class),
                        "",
                        "",
                        "",
                        ""
                );
            } catch (CreateModelException e) {
                return null;
            }
        }
    };

}
