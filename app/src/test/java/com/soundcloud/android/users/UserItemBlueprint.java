package com.soundcloud.android.users;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(UserItem.class)
public class UserItemBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return UserItem.from(ModelFixtures.create(ApiUser.class).toPropertySet());
        }
    };


}
