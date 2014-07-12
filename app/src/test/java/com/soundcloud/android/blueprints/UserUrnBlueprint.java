package com.soundcloud.android.blueprints;

        import com.soundcloud.android.model.Urn;
        import com.soundcloud.android.users.UserUrn;
        import com.tobedevoured.modelcitizen.annotation.Blueprint;
        import com.tobedevoured.modelcitizen.annotation.NewInstance;
        import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(UserUrn.class)
public class UserUrnBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return Urn.forUser(runningId++);
        }
    };

}
