package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(User.class)
public class UserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return User.create(Urn.forUser(runningId++), "user"+ runningId, "country", "city", 100, true,
                               Optional.<String>absent(), Optional.<String>absent());
        }
    };
}
