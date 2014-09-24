package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(Urn.class)
public class UserUrnBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return Urn.forUser(runningId++);
        }
    };

}
