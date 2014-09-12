package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Date;

@Blueprint(Comment.class)
public class CommentBlueprint {

    private static long runningId = 1L;

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final Comment comment = new Comment();
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
