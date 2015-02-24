package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.comments.Comment;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;

import java.util.Date;

@Blueprint(Comment.class)
public class CommentBlueprint {

    @Default String text = "a comment";

    @Default(force = true) long timestamp = 12345L;

    @Default String username = "test user";

    @Default Date date = new Date();

}
