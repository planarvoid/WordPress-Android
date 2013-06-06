package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.User;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;
import com.tobedevoured.modelcitizen.field.FieldCallback;

@Blueprint(User.class)
public class UserBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new User(runningId++);
        }
    };

    @Default
    FieldCallback username = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((User) referenceModel).getId();
        }
    };

}
