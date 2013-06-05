package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.SuggestedUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(SuggestedUser.class)
public class SuggestedUserBlueprint {

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new SuggestedUser("soundcloud:users:1");
        }
    };

    @Default
    String username = "Skrillex";
}
