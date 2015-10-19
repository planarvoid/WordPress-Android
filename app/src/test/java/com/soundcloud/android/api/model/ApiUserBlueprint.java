package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;
import com.tobedevoured.modelcitizen.callback.FieldCallback;

@Blueprint(ApiUser.class)
public class ApiUserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final ApiUser apiUser = new ApiUser(Urn.forUser(runningId++));
            apiUser.setFollowersCount(100);
            return apiUser;
        }
    };

    @Default
    FieldCallback username = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((ApiUser) referenceModel).getId();
        }
    };

    @Default
    String country = "Country";

}
