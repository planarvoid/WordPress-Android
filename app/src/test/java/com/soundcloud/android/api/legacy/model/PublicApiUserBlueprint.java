package com.soundcloud.android.api.legacy.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;
import com.tobedevoured.modelcitizen.callback.FieldCallback;

@Deprecated
@Blueprint(PublicApiUser.class)
public class PublicApiUserBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final PublicApiUser publicApiUser = new PublicApiUser("soundcloud:users:" + runningId++);
            // these have to go here because the setter has a differen param than the return type,
            // and ModelCitizen if you use the annotation
            publicApiUser.setDescription("description");
            publicApiUser.setWebsite("http://website-url.com");
            publicApiUser.setWebsiteTitle("website title");
            publicApiUser.setDiscogsName("discogs name");
            publicApiUser.setMyspaceName("myspace name");
            return publicApiUser;
        }
    };

    @Default
    FieldCallback username = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((PublicApiUser) referenceModel).getId();
        }
    };

    @Default
    FieldCallback permalink = new FieldCallback() {
        @Override
        public Object get(Object referenceModel) {
            return "user" + ((PublicApiUser) referenceModel).getId();
        }
    };

    @Default
    String avatarUrl = "http://i1.sndcdn.com/avatars-000001552142-pbw8yd-large.jpg?142a848";

    @Default
    String country = "Germany";

}
