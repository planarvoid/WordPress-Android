package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.ExploreTracksSuggestion;
import com.soundcloud.android.model.UserSummary;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(ExploreTracksSuggestion.class)
public class ExploreTracksSuggestionBlueprint {


    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ExploreTracksSuggestion("soundcloud:sounds:4L");
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    UserSummary user;
}
