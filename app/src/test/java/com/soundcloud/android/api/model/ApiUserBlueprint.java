package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;
import com.tobedevoured.modelcitizen.callback.FieldCallback;

import java.util.Date;

@Blueprint(ApiUser.class)
public class ApiUserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final ApiUser apiUser = new ApiUser(Urn.forUser(runningId++));
            apiUser.setFollowersCount(100);
            apiUser.setAvatarUrlTemplate("https://i1.sndcdn.com/avatars-" + runningId + "-{size}.jpg");
            apiUser.setVisualUrlTemplate("https://i1.sndcdn.com/visuals-" + runningId + "-{size}.jpg");
            apiUser.setFirstName("sound");
            apiUser.setLastName("cloud");
            apiUser.setCreatedAt(new Date(1476342997));
            return apiUser;
        }
    };

    @Default
    FieldCallback permalink = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user-permalink" + ((ApiUser) referenceModel).getId();
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

    @Default
    String city = "City";

}
