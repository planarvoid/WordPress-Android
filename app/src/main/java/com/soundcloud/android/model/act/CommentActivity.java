package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;

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
        comment.track = SoundCloudApplication.MODEL_MANAGER.getTrack(comment.track_id);
        comment.user = SoundCloudApplication.MODEL_MANAGER.getUser(comment.user_id);
    }

    @Override
    public Type getType() {
        return Type.COMMENT;
    }

    @Override
    public Track getTrack() {
        return comment.track;
    }

    @Override
    public User getUser() {
        return comment.user;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @Override
    public void cacheDependencies() {
        comment.user = SoundCloudApplication.MODEL_MANAGER.cache(comment.user, ScResource.CacheUpdateMode.MINI);
        comment.track = SoundCloudApplication.MODEL_MANAGER.cache(comment.track, ScResource.CacheUpdateMode.MINI);
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Activities.COMMENT_ID, comment.id);
        return cv;
    }

    @Override
    public List<ScResource> getDependentModels() {
        List<ScResource> models = super.getDependentModels();
        models.add(comment);
        return models;
    }

    @Override
    public ScResource getRefreshableResource() {
        // TODO, comment refreshing?
        return comment.user;
    }
}
