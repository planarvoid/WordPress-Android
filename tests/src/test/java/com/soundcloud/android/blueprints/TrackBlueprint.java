package com.soundcloud.android.blueprints;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

@Blueprint(Track.class)
public class TrackBlueprint {

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new Track(1L);
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    User user;
}
