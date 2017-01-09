package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;
import com.tobedevoured.modelcitizen.callback.FieldCallback;

@Blueprint(ApiUser.class)
public class ApiUserBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final ApiUser apiUser = new ApiUser(Urn.forUser(ModelFixtures.runningUserId++));
            apiUser.setFollowersCount(100);
            apiUser.setAvatarUrlTemplate("https://i1.sndcdn.com/avatars-" + ModelFixtures.runningUserId + "-{size}.jpg");
            apiUser.setVisualUrlTemplate("https://i1.sndcdn.com/visuals-" + ModelFixtures.runningUserId + "-{size}.jpg");
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
