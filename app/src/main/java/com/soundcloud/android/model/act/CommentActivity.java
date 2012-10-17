package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    public Comment comment;

    // for deserialization
    public CommentActivity() { }

    public CommentActivity(Cursor cursor) {
        super(cursor);
        comment = new Comment(cursor, true);
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


    @Override @JsonIgnore
    public void setCachedTrack(Track track) {
        comment.track_id = track.id;
        comment.track = track;
    }

    @Override @JsonIgnore
    public void setCachedUser(User user) {
        // nop
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
}
