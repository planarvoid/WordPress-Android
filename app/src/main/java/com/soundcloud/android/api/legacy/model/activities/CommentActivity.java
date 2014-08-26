package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropertySet;

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
    public PublicApiUser getUser() {
        return comment.user;
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(TableColumns.Activities.COMMENT_ID, comment.getId());
        return cv;
    }

    @Override
    public List<PublicApiResource> getDependentModels() {
        List<PublicApiResource> models = super.getDependentModels();
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

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_COMMENT)
                .put(ActivityProperty.SOUND_TITLE, comment.track.getTitle());
    }
}
