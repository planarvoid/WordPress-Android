package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Date;

@Blueprint(PublicApiTrack.class)
public class TrackBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new PublicApiTrack(runningId++);
        }
    };

    @Default
    String title = "new track " + System.currentTimeMillis();

    @Mapped
    PublicApiUser user;

    @Default
    Date createdAt = new Date();
}
