package com.soundcloud.android.api.legacy.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(PublicApiComment.class)
public class PublicApiCommentBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final PublicApiComment comment = new PublicApiComment();
            comment.setId(runningId++);
            return comment;
        }
    };


    @Mapped
    PublicApiUser user;

    @Mapped
    PublicApiTrack track;

    @Default()
    Date createdAt = new Date();

    @Default
    String body = "What is this, clownstep?";


}
