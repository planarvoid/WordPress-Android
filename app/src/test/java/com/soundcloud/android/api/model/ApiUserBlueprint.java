package com.soundcloud.android.api.model;

import com.soundcloud.android.testsupport.UserFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(ApiUser.class)
public class ApiUserBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return UserFixtures.apiUser();
        }
    };

}
