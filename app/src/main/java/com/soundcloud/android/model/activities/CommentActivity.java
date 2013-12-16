package com.soundcloud.android.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.storage.provider.DBHelper;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

public class CommentActivity extends Activity {
    @JsonProperty public Comment comment;

    // for deserialization
    public CommentActivity() {
        super();
    }

    public CommentActivity(Cursor cursor) {
        super(cursor);
        comment = new Comment(cursor, true);
        comment.track = SoundCloudApplication.sModelManager.getTrack(comment.track_id);
        comment.user = SoundCloudApplication.sModelManager.getUser(comment.user_id);
    }

    @Override
    public Type getType() {
        return Type.COMMENT;
    }

    @Override
    public Playable getPlayable() {
        return comment.track;
    }

    @Override
    public User getUser() {
        return comment.user;
    }

    @Override
    public void cacheDependencies() {
        comment.user = SoundCloudApplication.sModelManager.cache(comment.user, ScResource.CacheUpdateMode.MINI);
        comment.track = SoundCloudApplication.sModelManager.cache(comment.track, ScResource.CacheUpdateMode.MINI);
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Activities.COMMENT_ID, comment.getId());
        return cv;
    }

    @Override
    public List<ScResource> getDependentModels() {
        List<ScResource> models = super.getDependentModels();
        models.add(comment);
        return models;
    }

    @Override
    public Refreshable getRefreshableResource() {
        // TODO, comment refreshing?
        return comment.user;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }
}
