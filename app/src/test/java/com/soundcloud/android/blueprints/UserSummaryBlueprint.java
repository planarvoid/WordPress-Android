package com.soundcloud.android.blueprints;

import com.soundcloud.android.api.model.ApiUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;
import com.tobedevoured.modelcitizen.field.FieldCallback;

@Blueprint(ApiUser.class)
public class UserSummaryBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiUser("soundcloud:users:" + runningId++);
        }
    };

    @Default
    FieldCallback username = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((ApiUser) referenceModel).getId();
        }
    };

}
