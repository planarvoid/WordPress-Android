package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(User.class)
public class UserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final User user = User.create(Urn.forUser(runningId++), "user"+ runningId, "country", "city", 100, true);
            return user;
        }
    };
}
