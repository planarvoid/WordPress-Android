package com.soundcloud.android.model;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.Date;

@Blueprint(Comment.class)
public class CommentBlueprint {

    private static long runningId = 1L;

    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            final Comment comment = new Comment();
            comment.setId(runningId++);
            return comment;
        }
    };


    @Mapped
    User user;

    @Mapped
    Track track;

    @Default()
    Date createdAt = new Date();

    @Default
    String body = "What is this, clownstep?";


}
